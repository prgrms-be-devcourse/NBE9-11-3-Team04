package com.back.devc.global.initData

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AdminAccountInitData(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Bean
    fun adminAccountInitRunner(): ApplicationRunner {
        return ApplicationRunner {
            if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
                return@ApplicationRunner
            }

            val encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD)
                ?: throw IllegalStateException("관리자 비밀번호 인코딩에 실패했습니다.")

            val nickname = resolveUniqueNickname(ADMIN_NICKNAME_BASE)

            val admin = Member.createLocalAdminMember(
                ADMIN_EMAIL,
                encodedPassword,
                nickname
            )

            memberRepository.save(admin)
        }
    }

    private fun resolveUniqueNickname(base: String): String {
        if (!memberRepository.existsByNickname(base)) {
            return base
        }

        var sequence = 1
        var candidate = base + sequence
        while (memberRepository.existsByNickname(candidate)) {
            sequence++
            candidate = base + sequence
        }

        return candidate
    }

    companion object {
        private const val ADMIN_EMAIL = "admin@test.com"
        private const val ADMIN_PASSWORD = "admin123@"
        private const val ADMIN_NICKNAME_BASE = "admin"
    }
}