package com.back.devc.global.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String ERROR_CANCELLED = "OAUTH2_CANCELLED";
    private static final String ERROR_INVALID_STATE = "OAUTH2_INVALID_STATE";
    private static final String ERROR_LOGIN_FAILED = "OAUTH2_LOGIN_FAILED";

    private final OAuth2RedirectUrlResolver redirectUrlResolver;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = mapErrorCode(exception);
        log.warn("OAuth2 login failure. errorCode={}, message={}", errorCode, exception.getMessage(), exception);
        response.sendRedirect(redirectUrlResolver.buildFailureUrl(errorCode));
    }

    private String mapErrorCode(AuthenticationException exception) {
        if (!(exception instanceof OAuth2AuthenticationException oauth2Exception)) {
            return ERROR_LOGIN_FAILED;
        }

        String rawCode = oauth2Exception.getError().getErrorCode();
        if (rawCode == null) {
            return ERROR_LOGIN_FAILED;
        }

        return switch (rawCode) {
            case "access_denied" -> ERROR_CANCELLED;
            case "authorization_request_not_found", "invalid_state_parameter" -> ERROR_INVALID_STATE;
            default -> ERROR_LOGIN_FAILED;
        };
    }
}
