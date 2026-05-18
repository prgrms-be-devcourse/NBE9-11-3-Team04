package com.back.devc.global.security.oauth2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

@Component
class OAuth2RedirectUrlResolver(
    @Value("\${custom.oauth2.frontend-success-url:http://localhost:3000/oauth/callback}")
    private val frontendSuccessUrl: String,

    @Value("\${custom.oauth2.frontend-failure-url:http://localhost:3000/login}")
    private val frontendFailureUrl: String,

    @Value("\${custom.oauth2.frontend-signup-url:http://localhost:3000/oauth/signup}")
    private val frontendSignupUrl: String,

    @Value("\${custom.oauth2.allowed-redirect-uris:http://localhost:3000/login,http://localhost:3000/oauth/signup,http://localhost:3000/oauth/callback}")
    private val allowedRedirectUrisCsv: String
) {

    fun buildSuccessUrl(provider: String, authCode: String): String {
        val baseUrl = resolveAllowedOrFallback(frontendSuccessUrl, frontendFailureUrl)

        return UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("oauth", "success")
            .queryParam("provider", provider)
            .queryParam("code", authCode)
            .build()
            .encode()
            .toUriString()
    }

    fun buildFailureUrl(errorCode: String): String {
        val baseUrl = resolveAllowedOrFallback(frontendFailureUrl, frontendSuccessUrl)

        return UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("oauth", "error")
            .queryParam("errorCode", errorCode)
            .build()
            .encode()
            .toUriString()
    }

    fun buildSignupUrl(provider: String): String {
        val baseUrl = resolveAllowedOrFallback(frontendSignupUrl, frontendFailureUrl)

        return UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("oauth", "pending_signup")
            .queryParam("provider", provider)
            .build()
            .encode()
            .toUriString()
    }

    private fun resolveAllowedOrFallback(primary: String?, secondary: String?): String {
        val allowed = allowedUriMap()

        val normalizedPrimary = normalize(primary)
        if (normalizedPrimary != null && allowed.containsKey(normalizedPrimary)) {
            return allowed.getValue(normalizedPrimary)
        }

        val normalizedSecondary = normalize(secondary)
        if (normalizedSecondary != null && allowed.containsKey(normalizedSecondary)) {
            return allowed.getValue(normalizedSecondary)
        }

        if (allowed.isNotEmpty()) {
            return allowed.values.first()
        }

        return "http://localhost:3000/oauth/callback"
    }

    private fun allowedUriMap(): Map<String, String> {
        val map = LinkedHashMap<String, String>()

        for (raw in allowedRedirectUrisCsv.split(",")) {
            val candidate = raw.trim()
            if (candidate.isBlank()) {
                continue
            }

            val normalized = normalize(candidate)
            if (normalized != null) {
                map.putIfAbsent(normalized, candidate)
            }
        }

        return map
    }

    private fun normalize(uriString: String?): String? {
        if (uriString.isNullOrBlank()) {
            return null
        }

        return try {
            val uri = URI.create(uriString.trim())

            if (uri.scheme == null || uri.host == null) {
                return null
            }

            val scheme = uri.scheme.lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https") {
                return null
            }

            val host = uri.host.lowercase(Locale.ROOT)
            val port = uri.port

            var path = uri.path
            if (path.isNullOrBlank()) {
                path = "/"
            }

            "$scheme://$host${if (port > -1) ":$port" else ""}$path"
        } catch (e: Exception) {
            null
        }
    }
}