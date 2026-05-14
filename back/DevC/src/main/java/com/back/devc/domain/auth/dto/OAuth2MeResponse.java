package com.back.devc.domain.auth.dto;

import java.util.List;
import java.util.Map;

public record OAuth2MeResponse(
        boolean authenticated,
        String name,
        List<String> authorities,
        Map<String, Object> attributes
) {
}
