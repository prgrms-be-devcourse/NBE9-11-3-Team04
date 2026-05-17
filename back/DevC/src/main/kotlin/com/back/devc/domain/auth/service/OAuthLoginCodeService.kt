package com.back.devc.domain.auth.service

import com.back.devc.domain.member.member.entity.Member
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class OAuthLoginCodeService {

    companion object {
        private const val CODE_TTL_SECONDS = 120L
    }

    private val codeStore = ConcurrentHashMap<String, CodeEntry>()

    // OAuth 로그인 시, 프론트엔드에서 사용할 로그인 코드를 발급하는 메서드
    fun issue(member: Member): String {
        cleanupExpired()

        val code = UUID.randomUUID().toString().replace("-", "")
        val expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS)

        val userId = member.userId
            ?: throw IllegalStateException("OAuth 로그인 코드를 발급할 회원 ID가 없습니다.")

        codeStore[code] = CodeEntry(userId, expiresAt)
        return code
    }

    // 프론트엔드에서 전달받은 로그인 코드를 소비하여, 해당 코드가 유효한 경우 회원 ID를 반환하는 메서드
    fun consume(code: String?): Optional<Long> {
        if (code.isNullOrBlank()) {
            return Optional.empty()
        }

        val entry = codeStore.remove(code.trim())
            ?: return Optional.empty()

        if (entry.expiresAt.isBefore(Instant.now())) {
            return Optional.empty()
        }

        return Optional.of(entry.userId)
    }

    // 만료된 코드들을 정기적으로 정리하는 메서드
    private fun cleanupExpired() {
        val now = Instant.now()
        codeStore.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    // 로그인 코드와 해당 코드에 연결된 회원 ID 및 만료 시간을 저장하는 내부 데이터 클래스
    private data class CodeEntry(
        val userId: Long,
        val expiresAt: Instant
    )
}