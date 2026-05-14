package com.back.devc.domain.auth.service;

import com.back.devc.domain.member.member.entity.Member;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthLoginCodeService {

    private static final long CODE_TTL_SECONDS = 120L;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    public String issue(Member member) {
        cleanupExpired();

        String code = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS);

        codeStore.put(code, new CodeEntry(member.getUserId(), expiresAt));
        return code;
    }

    public Optional<Long> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        CodeEntry entry = codeStore.remove(code.trim());
        if (entry == null) {
            return Optional.empty();
        }

        if (entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        return Optional.of(entry.userId());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        codeStore.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record CodeEntry(Long userId, Instant expiresAt) {
    }
}