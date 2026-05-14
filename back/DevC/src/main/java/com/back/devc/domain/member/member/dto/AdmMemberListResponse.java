// AdminMemberListResponse.java
package com.back.devc.domain.member.member.dto;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;

import java.time.LocalDateTime;

public record AdmMemberListResponse(
        Long userId,
        String email,
        String nickname,
        long postCount,         // 작성한 게시글 수
        long commentCount,      // 작성한 댓글 수
        MemberStatus status,
        LocalDateTime createdAt,
         LocalDateTime suspendedUntil
) {
    public static AdmMemberListResponse of(Member member, long postCount, long commentCount) {
        return new AdmMemberListResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname(),
                postCount,
                commentCount,
                member.getStatus(),
                member.getCreatedAt(),
                member.getSuspendedUntil()
        );
    }
}