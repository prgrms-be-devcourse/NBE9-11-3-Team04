package com.back.devc.global.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class OAuth2RedirectUrlResolver {

    @Value("${custom.oauth2.frontend-success-url:http://localhost:3000/oauth/callback}")
    private String frontendSuccessUrl;

    @Value("${custom.oauth2.frontend-failure-url:http://localhost:3000/login}")
    private String frontendFailureUrl;

    @Value("${custom.oauth2.frontend-signup-url:http://localhost:3000/oauth/signup}")
    private String frontendSignupUrl;

    @Value("${custom.oauth2.allowed-redirect-uris:http://localhost:3000/login,http://localhost:3000/oauth/signup,http://localhost:3000/oauth/callback}")
    private String allowedRedirectUrisCsv;

    public String buildSuccessUrl(String provider, String authCode) {
        String baseUrl = resolveAllowedOrFallback(frontendSuccessUrl, frontendFailureUrl);

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("oauth", "success")
                .queryParam("provider", provider)
                .queryParam("code", authCode)
                .build()
                .encode()
                .toUriString();
    }

    public String buildFailureUrl(String errorCode) {
        String baseUrl = resolveAllowedOrFallback(frontendFailureUrl, frontendSuccessUrl);

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("oauth", "error")
                .queryParam("errorCode", errorCode)
                .build()
                .encode()
                .toUriString();
    }

    public String buildSignupUrl(String provider) {
        String baseUrl = resolveAllowedOrFallback(frontendSignupUrl, frontendFailureUrl);

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("oauth", "pending_signup")
                .queryParam("provider", provider)
                .build()
                .encode()
                .toUriString();
    }

    private String resolveAllowedOrFallback(String primary, String secondary) {
        Map<String, String> allowed = allowedUriMap();

        String normalizedPrimary = normalize(primary);
        if (normalizedPrimary != null && allowed.containsKey(normalizedPrimary)) {
            return allowed.get(normalizedPrimary);
        }

        String normalizedSecondary = normalize(secondary);
        if (normalizedSecondary != null && allowed.containsKey(normalizedSecondary)) {
            return allowed.get(normalizedSecondary);
        }

        if (!allowed.isEmpty()) {
            return allowed.values().iterator().next();
        }

        return "http://localhost:3000/oauth/callback";
    }

    private Map<String, String> allowedUriMap() {
        Map<String, String> map = new LinkedHashMap<>();

        for (String raw : allowedRedirectUrisCsv.split(",")) {
            String candidate = raw.trim();
            if (candidate.isBlank()) {
                continue;
            }

            String normalized = normalize(candidate);
            if (normalized != null) {
                map.putIfAbsent(normalized, candidate);
            }
        }

        return map;
    }

    private String normalize(String uriString) {
        if (uriString == null || uriString.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(uriString.trim());

            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }

            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();

            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            return scheme + "://" + host + (port > -1 ? ":" + port : "") + path;
        } catch (Exception e) {
            return null;
        }
    }
}
