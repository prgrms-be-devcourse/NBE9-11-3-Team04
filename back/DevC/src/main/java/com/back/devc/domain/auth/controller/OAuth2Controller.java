package com.back.devc.domain.auth.controller;

import com.back.devc.domain.auth.dto.OAuth2MeResponse;
import com.back.devc.domain.auth.dto.login.LoginResponse;
import com.back.devc.domain.auth.dto.oauth.OAuthExchangeRequest;
import com.back.devc.domain.auth.dto.oauth.OAuthPendingSignup;
import com.back.devc.domain.auth.dto.oauth.OAuthSignupCompleteRequest;
import com.back.devc.domain.auth.service.OAuth2MemberService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.AuthSuccessCode;
import com.back.devc.global.security.jwt.AuthCookieService;
import com.back.devc.global.security.oauth2.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {

    private final OAuth2MemberService oAuth2MemberService;
    private final AuthCookieService authCookieService;

    @Value("${custom.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    // OAuth2 로그인 상태/회원가입 대기 상태를 조회한다.
    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<OAuth2MeResponse>> me(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest httpServletRequest
    ) {
        OAuth2MeResponse body;
        HttpSession session = httpServletRequest.getSession(false);

        if (session != null) {
            Object raw = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY);
            if (raw instanceof OAuthPendingSignup pending) {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("pendingSignup", true);
                attributes.put("provider", pending.provider());
                attributes.put("providerUserId", pending.providerUserId());
                attributes.put("email", pending.emailFromProvider());
                attributes.put("login", pending.loginFromProvider());

                body = new OAuth2MeResponse(false, null, List.of(), attributes);
                AuthSuccessCode successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS;
                return ResponseEntity
                        .status(successCode.getStatus())
                        .body(SuccessResponse.of(successCode, body));
            }
        }

        if (oauth2User == null) {
            body = new OAuth2MeResponse(false, null, List.of(), Map.of("pendingSignup", false));
            AuthSuccessCode successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS;
            return ResponseEntity
                    .status(successCode.getStatus())
                    .body(SuccessResponse.of(successCode, body));
        }

        List<String> authorities = oauth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> attributes = new LinkedHashMap<>(oauth2User.getAttributes());
        attributes.put("pendingSignup", false);
        body = new OAuth2MeResponse(true, oauth2User.getName(), authorities, attributes);

        AuthSuccessCode successCode = AuthSuccessCode.OAUTH_200_ME_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // OAuth2 로그인 코드를 교환해 로그인 정보를 반환하고 토큰 쿠키를 설정한다.
    @PostMapping("/exchange")
    public ResponseEntity<SuccessResponse<LoginResponse>> exchange(
            @Valid @RequestBody OAuthExchangeRequest request,
            HttpServletResponse response
    ) {
        LoginResponse body = oAuth2MemberService.exchangeLoginCode(request.code());
        authCookieService.setAccessTokenCookie(response, body.accessToken(), accessTokenExpirationSeconds);

        AuthSuccessCode successCode = AuthSuccessCode.OAUTH_200_EXCHANGE_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // 세션의 pendingSignup 정보를 이용해 OAuth2 회원가입을 완료하고 로그인 정보를 반환한다.
    @PostMapping("/signup/complete")
    public ResponseEntity<SuccessResponse<LoginResponse>> completeSignup(
            @Valid @RequestBody OAuthSignupCompleteRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response
    ) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session == null) {
            throw new ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_EXPIRED);
        }

        Object raw = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY);
        if (!(raw instanceof OAuthPendingSignup pending)) {
            throw new ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED);
        }

        LoginResponse body = oAuth2MemberService.completeSignupAndIssueToken(pending, request.nickname());
        session.removeAttribute(OAuth2LoginSuccessHandler.PENDING_SIGNUP_SESSION_KEY);
        authCookieService.setAccessTokenCookie(response, body.accessToken(), accessTokenExpirationSeconds);

        AuthSuccessCode successCode = AuthSuccessCode.OAUTH_201_SIGNUP_COMPLETE_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }
}