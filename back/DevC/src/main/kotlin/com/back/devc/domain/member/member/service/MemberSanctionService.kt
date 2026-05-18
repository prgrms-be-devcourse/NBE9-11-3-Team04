package com.back.devc.domain.member.member.service

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MemberSanctionService {

    fun warn(member: Member) {
        log.info("회원 경고 처리 시작 - userId={}, currentStatus={}", member.userId, member.status)

        if (member.status == MemberStatus.ACTIVE) {
            member.updateStatus(MemberStatus.WARNED)
            log.info("회원 경고 처리 완료 - userId={}, changedStatus={}", member.userId, member.status)
            return
        }

        log.info("회원 경고 처리 생략 - ACTIVE 상태가 아님, userId={}, currentStatus={}", member.userId, member.status)
    }

    fun suspend(member: Member, days: Int?) {
        log.info(
            "회원 정지 처리 시작 - userId={}, currentStatus={}, requestedDays={}, currentSuspendedUntil={}",
            member.userId,
            member.status,
            days,
            member.suspendedUntil,
        )

        val validDays = days?.takeIf { it > 0 } ?: 1
        if (days == null || days <= 0) {
            log.warn("회원 정지 일수 보정 - userId={}, requestedDays={}, validDays={}", member.userId, days, validDays)
        } else {
            log.debug("회원 정지 일수 확인 - userId={}, validDays={}", member.userId, validDays)
        }

        member.updateStatus(MemberStatus.SUSPENDED)

        val currentUntil = member.suspendedUntil
        val now = LocalDateTime.now()

        member.suspendedUntil = if (currentUntil != null && currentUntil.isAfter(now)) {
            currentUntil.plusDays(validDays.toLong()).also {
                log.info(
                    "회원 정지 기간 누적 처리 - userId={}, previousSuspendedUntil={}, validDays={}, newSuspendedUntil={}",
                    member.userId,
                    currentUntil,
                    validDays,
                    it,
                )
            }
        } else {
            now.plusDays(validDays.toLong()).also {
                log.info("회원 신규 정지 처리 - userId={}, validDays={}, newSuspendedUntil={}", member.userId, validDays, it)
            }
        }

        log.info(
            "회원 정지 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
            member.userId,
            member.status,
            member.suspendedUntil,
        )
    }

    fun blacklist(member: Member) {
        log.info(
            "회원 블랙리스트 처리 시작 - userId={}, currentStatus={}, currentSuspendedUntil={}",
            member.userId,
            member.status,
            member.suspendedUntil,
        )

        member.updateStatus(MemberStatus.BLACKLISTED)
        member.suspendedUntil = null

        log.info(
            "회원 블랙리스트 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
            member.userId,
            member.status,
            member.suspendedUntil,
        )
    }

    fun activate(member: Member) {
        log.info(
            "회원 활성화 처리 시작 - userId={}, currentStatus={}, currentSuspendedUntil={}",
            member.userId,
            member.status,
            member.suspendedUntil,
        )

        member.updateStatus(MemberStatus.ACTIVE)
        member.suspendedUntil = null

        log.info(
            "회원 활성화 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
            member.userId,
            member.status,
            member.suspendedUntil,
        )
    }

    fun apply(member: Member, status: MemberStatus, days: Int?) {
        log.info(
            "회원 제재 적용 시작 - userId={}, currentStatus={}, targetStatus={}, days={}, currentSuspendedUntil={}",
            member.userId,
            member.status,
            status,
            days,
            member.suspendedUntil,
        )

        when (status) {
            MemberStatus.ACTIVE -> activate(member)
            MemberStatus.WARNED -> warn(member)
            MemberStatus.SUSPENDED -> suspend(member, days)
            MemberStatus.BLACKLISTED -> blacklist(member)
            MemberStatus.WITHDRAWN -> Unit
        }

        log.info(
            "회원 제재 적용 완료 - userId={}, targetStatus={}, changedStatus={}, suspendedUntil={}",
            member.userId,
            status,
            member.status,
            member.suspendedUntil,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(MemberSanctionService::class.java)
    }
}

