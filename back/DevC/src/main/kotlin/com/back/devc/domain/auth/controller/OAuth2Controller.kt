package com.back.devc.domain.auth.controller

import com.back.devc.domain.auth.dto.OAuth2MeResponse
import com.back.devc.domain.auth.dto.login.LoginResponse
import com.back.devc.domain.auth.dto.oauth.OAuthExchangeRequest
import com.back.devc.domain.auth.dto.oauth.OAuthPendingSignup
import com.back.devc.domain.auth.dto.oauth.OAuthSignupCompleteRequest
import com.back.devc.domain.auth.service.OAuth2MemberService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.AuthSuccessCode
import com.back.devc.global.security.jwt.AuthCookieService
import com.back.devc.global.security.oauth2.OAuth2LoginSuccessHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/oauth2")
class OAuth2Controller(
    private val oAuth2MemberService: OAuth2MemberService,
    private val authCookieService: AuthCookieService
) {

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal oauth2User: OAuth2User?,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<SuccessResponse<OAuth2MeResponse>> {
        val session = httpServletRequest.getSession(false)

        if (session != null) {
            val raw = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY)
            if (raw is OAuthPendingSignup) {
                val attributes = linkedMapOf<String, Any>(
                    "pendingSignup" to true,
                    "provider" to raw.provider,
                    "providerUserId" to raw.providerUserId,
                    "email" to raw.emailFromProvider,
                    "login" to raw.loginFromProvider
                )

                val body = OAuth2MeResponse(false, null, emptyList(), attributes)
                val successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS

                return ResponseEntity
                    .status(successCode.status)
                    .body(SuccessResponse.of(successCode, body))
            }
        }

        if (oauth2User == null) {
            val body = OAuth2MeResponse(false, null, emptyList(), mapOf("pendingSignup" to false))
            val successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS

            return ResponseEntity
                .status(successCode.status)
                .body(SuccessResponse.of(successCode, body))
        }

        val authorities = oauth2User.authorities
            .map(GrantedAuthority::getAuthority)

        val attributes = LinkedHashMap<String, Any>(oauth2User.attributes)
        attributes["pendingSignup"] = false

        val body = OAuth2MeResponse(true, oauth2User.name, authorities, attributes)
        val successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    @PostMapping("/exchange")
    fun exchange(
        @RequestBody @Valid request: OAuthExchangeRequest,
        response: HttpServletResponse
    ): ResponseEntity<SuccessResponse<LoginResponse>> {
        val body = oAuth2MemberService.exchangeLoginCode(request.code)
        authCookieService.setAccessTokenCookie(response, body.accessToken)

        val successCode = AuthSuccessCode.OAUTH_200_EXCHANGE_SUCCESS
        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    @PostMapping("/signup/complete")
    fun completeSignup(
        @RequestBody @Valid request: OAuthSignupCompleteRequest,
        httpServletRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<SuccessResponse<LoginResponse>> {
        val session = httpServletRequest.getSession(false)
            ?: throw ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_EXPIRED)

        val pending = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY)
                as? OAuthPendingSignup
            ?: throw ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED)

        val body = oAuth2MemberService.completeSignupAndIssueToken(pending, request.nickname)
        session.removeAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY)
        authCookieService.setAccessTokenCookie(response, body.accessToken)

        val successCode = AuthSuccessCode.OAUTH_201_SIGNUP_COMPLETE_SUCCESS
        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }
}