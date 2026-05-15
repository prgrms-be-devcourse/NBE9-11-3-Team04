package com.back.devc.domain.member.member.service;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class MemberSanctionService {

    // WARN
    public void warn(Member member) {
        log.info("회원 경고 처리 시작 - userId={}, currentStatus={}", member.getUserId(), member.getStatus());

        if (member.getStatus() == MemberStatus.ACTIVE) {
            member.updateStatus(MemberStatus.WARNED);
            log.info("회원 경고 처리 완료 - userId={}, changedStatus={}", member.getUserId(), member.getStatus());
            return;
        }

        log.info("회원 경고 처리 생략 - ACTIVE 상태가 아님, userId={}, currentStatus={}", member.getUserId(), member.getStatus());
    }

    // SUSPEND (기간 있음)
    public void suspend(Member member, Integer days) {
        log.info("회원 정지 처리 시작 - userId={}, currentStatus={}, requestedDays={}, currentSuspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                days,
                member.getSuspendedUntil());

        int validDays = (days != null && days > 0) ? days : 1;

        if (days == null || days <= 0) {
            log.warn("회원 정지 일수 보정 - userId={}, requestedDays={}, validDays={}", member.getUserId(), days, validDays);
        } else {
            log.debug("회원 정지 일수 확인 - userId={}, validDays={}", member.getUserId(), validDays);
        }

        member.updateStatus(MemberStatus.SUSPENDED);

        // 현재 제재 종료일이 남아있는지 확인
        LocalDateTime currentUntil = member.getSuspendedUntil();
        LocalDateTime now = LocalDateTime.now();

        if (currentUntil != null && currentUntil.isAfter(now)) {
            // 이미 정지 중이라면: 기존 종료일에 추가 (누적)
            LocalDateTime newSuspendedUntil = currentUntil.plusDays(validDays);
            member.setSuspendedUntil(newSuspendedUntil);
            log.info("회원 정지 기간 누적 처리 - userId={}, previousSuspendedUntil={}, validDays={}, newSuspendedUntil={}",
                    member.getUserId(),
                    currentUntil,
                    validDays,
                    newSuspendedUntil);
        } else {
            // 정지 중이 아니라면: 현재 시점부터 추가
            LocalDateTime newSuspendedUntil = now.plusDays(validDays);
            member.setSuspendedUntil(newSuspendedUntil);
            log.info("회원 신규 정지 처리 - userId={}, validDays={}, newSuspendedUntil={}",
                    member.getUserId(),
                    validDays,
                    newSuspendedUntil);
        }

        log.info("회원 정지 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                member.getSuspendedUntil());
    }

    // BLACKLIST
    public void blacklist(Member member) {
        log.info("회원 블랙리스트 처리 시작 - userId={}, currentStatus={}, currentSuspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                member.getSuspendedUntil());

        member.updateStatus(MemberStatus.BLACKLISTED);
        member.setSuspendedUntil(null);

        log.info("회원 블랙리스트 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                member.getSuspendedUntil());
    }

    // 제재 해제 (활성화)
    public void activate(Member member) {
        log.info("회원 활성화 처리 시작 - userId={}, currentStatus={}, currentSuspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                member.getSuspendedUntil());

        member.updateStatus(MemberStatus.ACTIVE);
        member.setSuspendedUntil(null); // 정지 기간 초기화

        log.info("회원 활성화 처리 완료 - userId={}, changedStatus={}, suspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                member.getSuspendedUntil());
    }

    public void apply(Member member, MemberStatus status, Integer days) {
        log.info("회원 제재 적용 시작 - userId={}, currentStatus={}, targetStatus={}, days={}, currentSuspendedUntil={}",
                member.getUserId(),
                member.getStatus(),
                status,
                days,
                member.getSuspendedUntil());

        switch (status) {
            case ACTIVE -> activate(member); // 추가
            case WARNED -> warn(member);
            case SUSPENDED -> suspend(member, days);
            case BLACKLISTED -> blacklist(member);
        }

        log.info("회원 제재 적용 완료 - userId={}, targetStatus={}, changedStatus={}, suspendedUntil={}",
                member.getUserId(),
                status,
                member.getStatus(),
                member.getSuspendedUntil());
    }
}