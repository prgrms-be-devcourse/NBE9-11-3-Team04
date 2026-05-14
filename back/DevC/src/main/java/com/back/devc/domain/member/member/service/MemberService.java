package com.back.devc.domain.member.member.service;

import com.back.devc.domain.member.member.dto.MemberWithdrawResponse;
import com.back.devc.domain.member.member.dto.MyInfoResponse;
import com.back.devc.domain.member.member.dto.PublicProfilePostResponse;
import com.back.devc.domain.member.member.dto.PublicProfileResponse;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        log.info("내 정보 조회 시작 - userId={}", userId);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("내 정보 조회 실패 - 회원 없음, userId={}", userId);
                    return new ApiException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        log.info("내 정보 조회 완료 - userId={}, email={}", member.getUserId(), member.getEmail());
        return new MyInfoResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }

    // 공개 프로필 조회 (최근 게시글 20개 포함)
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {
        log.info("공개 프로필 조회 시작 - userId={}", userId);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("공개 프로필 조회 실패 - 회원 없음, userId={}", userId);
                    return new ApiException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        List<PublicProfilePostResponse> posts = postRepository
                .findTop20ByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member)
                .stream()
                .map(post -> new PublicProfilePostResponse(
                        post.getPostId(),
                        post.getTitle(),
                        post.getLikeCount(),
                        post.getCommentCount(),
                        post.getCreatedAt()
                ))
                .toList();

        log.info("공개 프로필 조회 완료 - userId={}, postCount={}", member.getUserId(), posts.size());
        return new PublicProfileResponse(
                member.getUserId(),
                member.getNickname(),
                posts
        );
    }

    // 회원 탈퇴 처리
    @Transactional
    public MemberWithdrawResponse withdraw(Long userId) {
        log.info("회원 탈퇴 시작 - userId={}", userId);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 탈퇴 실패 - 회원 없음, userId={}", userId);
                    return new ApiException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        member.withdraw();
        log.info("회원 탈퇴 완료 - userId={}", member.getUserId());
        return new MemberWithdrawResponse(member.getUserId());
    }
}
