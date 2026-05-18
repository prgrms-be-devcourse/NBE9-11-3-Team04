package com.back.devc.domain.member.member.service

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@DisplayName("MemberSanctionService")
class MemberSanctionServiceTest {

    private val service = MemberSanctionService()

    @Test
    @DisplayName("ACTIVE 회원은 WARNED 상태로 경고 처리된다")
    fun warnActiveMember() {
        val member = member(MemberStatus.ACTIVE)

        service.warn(member)

        assertThat(member.status).isEqualTo(MemberStatus.WARNED)
    }

    @Test
    @DisplayName("ACTIVE가 아닌 회원은 경고 처리해도 상태가 유지된다")
    fun warnNonActiveMemberDoesNothing() {
        val member = member(MemberStatus.SUSPENDED)

        service.warn(member)

        assertThat(member.status).isEqualTo(MemberStatus.SUSPENDED)
    }

    @Test
    @DisplayName("정지 일수가 없거나 0 이하이면 1일 정지로 보정한다")
    fun suspendUsesOneDayWhenDaysInvalid() {
        val member = member(MemberStatus.ACTIVE)
        val before = LocalDateTime.now()

        service.suspend(member, 0)

        assertThat(member.status).isEqualTo(MemberStatus.SUSPENDED)
        assertThat(member.suspendedUntil).isAfter(before)
        assertThat(member.suspendedUntil).isBefore(LocalDateTime.now().plusDays(2))
    }

    @Test
    @DisplayName("이미 정지 중이면 기존 정지 종료일에 기간을 누적한다")
    fun suspendExtendsCurrentSuspension() {
        val currentUntil = LocalDateTime.now().plusDays(3)
        val member = member(MemberStatus.SUSPENDED, currentUntil)

        service.suspend(member, 2)

        assertThat(member.suspendedUntil).isEqualTo(currentUntil.plusDays(2))
    }

    @Test
    @DisplayName("블랙리스트 처리 시 정지 종료일을 초기화한다")
    fun blacklistClearsSuspension() {
        val member = member(MemberStatus.SUSPENDED, LocalDateTime.now().plusDays(3))

        service.blacklist(member)

        assertThat(member.status).isEqualTo(MemberStatus.BLACKLISTED)
        assertThat(member.suspendedUntil).isNull()
    }

    @Test
    @DisplayName("활성화 처리 시 정지 종료일을 초기화한다")
    fun activateClearsSuspension() {
        val member = member(MemberStatus.SUSPENDED, LocalDateTime.now().plusDays(3))

        service.activate(member)

        assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(member.suspendedUntil).isNull()
    }

    @Test
    @DisplayName("WITHDRAWN 대상 제재 적용은 상태를 변경하지 않는다")
    fun applyWithdrawnDoesNothing() {
        val member = member(MemberStatus.WITHDRAWN)

        service.apply(member, MemberStatus.WITHDRAWN, null)

        assertThat(member.status).isEqualTo(MemberStatus.WITHDRAWN)
    }

    private fun member(
        status: MemberStatus,
        suspendedUntil: LocalDateTime? = null,
    ): Member {
        return Member.createLocalMember("member$status@test.com", "password", "member$status").also {
            ReflectionTestUtils.setField(it, "userId", 1L)
            ReflectionTestUtils.setField(it, "status", status)
            it.suspendedUntil = suspendedUntil
        }
    }
}

