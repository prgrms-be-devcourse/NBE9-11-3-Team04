package com.back.devc.global.security.oauth2

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class OAuth2LoginFailureHandler(
    private val redirectUrlResolver: OAuth2RedirectUrlResolver
) : AuthenticationFailureHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val errorCode = mapErrorCode(exception)
        log.warn("OAuth2 login failure. errorCode={}, message={}", errorCode, exception.message, exception)
        response.sendRedirect(redirectUrlResolver.buildFailureUrl(errorCode))
    }

    private fun mapErrorCode(exception: AuthenticationException): String {
        if (exception !is OAuth2AuthenticationException) {
            return ERROR_LOGIN_FAILED
        }

        val rawCode = exception.error.errorCode ?: return ERROR_LOGIN_FAILED

        return when (rawCode) {
            "access_denied" -> ERROR_CANCELLED
            "authorization_request_not_found",
            "invalid_state_parameter" -> ERROR_INVALID_STATE
            else -> ERROR_LOGIN_FAILED
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OAuth2LoginFailureHandler::class.java)

        private const val ERROR_CANCELLED = "OAUTH2_CANCELLED"
        private const val ERROR_INVALID_STATE = "OAUTH2_INVALID_STATE"
        private const val ERROR_LOGIN_FAILED = "OAUTH2_LOGIN_FAILED"
    }
}