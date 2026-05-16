package com.back.devc.domain.member.member.repository

import com.back.devc.domain.member.member.entity.AuthProvider
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MemberRepository : JpaRepository<Member, Long> {

    fun existsByEmail(email: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun findByEmail(email: String): Optional<Member>

    fun findByProviderAndProviderUserId(
        provider: AuthProvider,
        providerUserId: String
    ): Optional<Member>

    fun findByNicknameContainingIgnoreCase(
        nickname: String,
        pageable: Pageable
    ): Page<Member>

    fun findByStatus(
        status: MemberStatus,
        pageable: Pageable
    ): Page<Member>

    fun findByNicknameContainingOrEmailContaining(
        nickname: String,
        email: String,
        pageable: Pageable
    ): Page<Member>

    fun findByStatusAndNicknameContainingOrStatusAndEmailContaining(
        status1: MemberStatus,
        nickname: String,
        status2: MemberStatus,
        email: String,
        pageable: Pageable
    ): Page<Member>
}