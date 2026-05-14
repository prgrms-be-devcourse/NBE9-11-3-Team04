package com.back.devc.global.initData;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccountInitData {

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin123@";
    private static final String ADMIN_NICKNAME_BASE = "admin";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner adminAccountInitRunner() {
        return args -> {
            if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
                return;
            }

            String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            String nickname = resolveUniqueNickname(ADMIN_NICKNAME_BASE);

            Member admin = Member.createLocalAdminMember(
                    ADMIN_EMAIL,
                    encodedPassword,
                    nickname
            );

            memberRepository.save(admin);
        };
    }

    private String resolveUniqueNickname(String base) {
        if (!memberRepository.existsByNickname(base)) {
            return base;
        }

        int sequence = 1;
        String candidate = base + sequence;
        while (memberRepository.existsByNickname(candidate)) {
            sequence++;
            candidate = base + sequence;
        }
        return candidate;
    }
}
