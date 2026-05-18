package com.back.devc.domain.member.member.util

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

@DisplayName("MemberDisplayUtil")
class MemberDisplayUtilTest {

    @Test
    @DisplayName("일반 회원은 닉네임을 표시한다")
    fun activeMemberDisplayName() {
        val member = member("active", MemberStatus.ACTIVE)

        val result = MemberDisplayUtil.getDisplayName(member)

        assertThat(result).isEqualTo("active")
    }

    @Test
    @DisplayName("탈퇴 회원은 탈퇴한 회원으로 표시한다")
    fun withdrawnMemberDisplayName() {
        val member = member("withdrawn", MemberStatus.WITHDRAWN)

        val result = MemberDisplayUtil.getDisplayName(member)

        assertThat(result).isEqualTo("탈퇴한 회원")
    }

    private fun member(nickname: String, status: MemberStatus): Member {
        return Member.createLocalMember("$nickname@test.com", "password", nickname).also {
            ReflectionTestUtils.setField(it, "status", status)
        }
    }
}

