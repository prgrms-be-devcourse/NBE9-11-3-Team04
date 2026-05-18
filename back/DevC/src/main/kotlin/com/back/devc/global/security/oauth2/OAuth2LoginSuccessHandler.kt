package com.back.devc.global.security.oauth2

import com.back.devc.domain.auth.service.OAuth2MemberService
import com.back.devc.domain.auth.service.OAuthLoginCodeService
import com.back.devc.domain.member.member.entity.MemberStatus
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class OAuth2LoginSuccessHandler(
    private val oAuth2MemberService: OAuth2MemberService,
    private val redirectUrlResolver: OAuth2RedirectUrlResolver,
    private val oAuthLoginCodeService: OAuthLoginCodeService
) : AuthenticationSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal
        if (principal !is OAuth2User) {
            response.sendRedirect(redirectUrlResolver.buildFailureUrl(ERROR_INVALID_PRINCIPAL))
            return
        }

        var provider = "unknown"
        if (authentication is OAuth2AuthenticationToken) {
            provider = authentication.authorizedClientRegistrationId
        }

        if (!isSupportedProvider(provider)) {
            response.sendRedirect(redirectUrlResolver.buildFailureUrl(ERROR_UNSUPPORTED_PROVIDER))
            return
        }

        try {
            val pending = oAuth2MemberService.buildPendingSignup(provider, principal)

            val existing = oAuth2MemberService.findMemberByProviderUserId(provider, pending.providerUserId)
            if (existing.isPresent) {
                val member = existing.get()

                if (member.status == MemberStatus.BLACKLISTED) {
                    response.sendRedirect(redirectUrlResolver.buildFailureUrl(ERROR_MEMBER_BLACKLISTED))
                    return
                }

                val session = request.getSession(false)
                if (session != null) {
                    session.removeAttribute(PENDING_SIGNUP_SESSION_KEY)
                }

                val authCode = oAuthLoginCodeService.issue(member)
                response.sendRedirect(redirectUrlResolver.buildSuccessUrl(provider, authCode))
                return
            }

            val session = request.getSession(true)
            session.setAttribute(PENDING_SIGNUP_SESSION_KEY, pending)

            response.sendRedirect(redirectUrlResolver.buildSignupUrl(provider))
        } catch (e: Exception) {
            log.error("OAuth2 success handler failed. provider={}", provider, e)
            response.sendRedirect(redirectUrlResolver.buildFailureUrl(ERROR_TOKEN_ISSUE))
        }
    }

    private fun isSupportedProvider(provider: String): Boolean {
        return "github".equals(provider, ignoreCase = true) ||
                "kakao".equals(provider, ignoreCase = true) ||
                "google".equals(provider, ignoreCase = true)
    }

    companion object {
        const val PENDING_SIGNUP_SESSION_KEY = "OAUTH2_PENDING_SIGNUP"

        private const val ERROR_INVALID_PRINCIPAL = "OAUTH2_INVALID_PRINCIPAL"
        private const val ERROR_MEMBER_BLACKLISTED = "OAUTH2_MEMBER_BLACKLISTED"
        private const val ERROR_UNSUPPORTED_PROVIDER = "OAUTH2_UNSUPPORTED_PROVIDER"
        private const val ERROR_TOKEN_ISSUE = "OAUTH2_TOKEN_ISSUE"

        private val log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)
    }
}