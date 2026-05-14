package com.back.devc.domain.member.member.service;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MemberSanctionService {

    // WARN
    public void warn(Member member) {
        if (member.getStatus() == MemberStatus.ACTIVE) {
            member.updateStatus(MemberStatus.WARNED);
        }
    }

    // SUSPEND (기간 있음)
    public void suspend(Member member, Integer days) {
        int validDays = (days != null && days > 0) ? days : 1;

        member.updateStatus(MemberStatus.SUSPENDED);

        // 현재 제재 종료일이 남아있는지 확인
        LocalDateTime currentUntil = member.getSuspendedUntil();
        LocalDateTime now = LocalDateTime.now();

        if (currentUntil != null && currentUntil.isAfter(now)) {
            // 이미 정지 중이라면: 기존 종료일에 추가 (누적)
            member.setSuspendedUntil(currentUntil.plusDays(validDays));
        } else {
            // 정지 중이 아니라면: 현재 시점부터 추가
            member.setSuspendedUntil(now.plusDays(validDays));
        }
    }

    // BLACKLIST
    public void blacklist(Member member) {
        member.updateStatus(MemberStatus.BLACKLISTED);
        member.setSuspendedUntil(null);
    }

    // 제재 해제 (활성화)
    public void activate(Member member) {
        member.updateStatus(MemberStatus.ACTIVE);
        member.setSuspendedUntil(null); // 정지 기간 초기화
    }

    public void apply(Member member, MemberStatus status, Integer days) {
        switch (status) {
            case ACTIVE -> activate(member); // 추가
            case WARNED -> warn(member);
            case SUSPENDED -> suspend(member, days);
            case BLACKLISTED -> blacklist(member);
        }
    }
}