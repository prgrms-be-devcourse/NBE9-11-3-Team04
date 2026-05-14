package com.back.devc.domain.member.member.service;

import com.back.devc.domain.member.member.dto.*;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmMemberService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 1. 회원 목록 조회 (N+1 제거 완료)
    public Page<AdmMemberListResponse> getMembers(AdmMemberListRequest request) {

        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Member> members = findMembers(request, pageable);

        List<Member> memberList = members.getContent();

        // 빈 페이지면 바로 반환
        if (memberList.isEmpty()) {
            return members.map(member -> AdmMemberListResponse.of(member, 0, 0));
        }

        // userId 목록 추출
        List<Long> userIds = memberList.stream()
                .map(Member::getUserId)
                .toList();

        // postCount Map 생성
        Map<Long, Long> postCountMap = postRepository.countPostsByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        CountResultDto::userId,
                        CountResultDto::count
                ));

        // commentCount Map 생성
        Map<Long, Long> commentCountMap = commentRepository.countCommentsByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        CountResultDto::userId,
                        CountResultDto::count
                ));

        // 최종 조립
        return members.map(member -> {
            long postCount = postCountMap.getOrDefault(member.getUserId(), 0L);
            long commentCount = commentCountMap.getOrDefault(member.getUserId(), 0L);

            return AdmMemberListResponse.of(member, postCount, commentCount);
        });
    }

    // 2. 회원 상세 조회 (단건은 count 쿼리 2개 정도는 허용 가능)
    public AdmMemberDetailResponse getMemberDetail(Long userId) {

        Member member = findMemberOrThrow(userId);
        validateActiveMember(member);

        long postCount = postRepository.countPostsByUserIds(List.of(userId))
                .stream()
                .findFirst()
                .map(CountResultDto::count)
                .orElse(0L);

        long commentCount = commentRepository.countCommentsByUserIds(List.of(userId))
                .stream()
                .findFirst()
                .map(CountResultDto::count)
                .orElse(0L);

        return AdmMemberDetailResponse.of(member, postCount, commentCount);
    }

    // 3. 회원 상태 변경
    @Transactional
    public AdmMemberDetailResponse updateMemberStatus(Long userId,
                                                      AdmMemberStatusUpdateRequest request) {

        Member member = findMemberOrThrow(userId);

        validateUpdatableMember(member);

        member.updateStatus(request.status());

        long postCount = postRepository.countPostsByUserIds(List.of(userId))
                .stream()
                .findFirst()
                .map(CountResultDto::count)
                .orElse(0L);

        long commentCount = commentRepository.countCommentsByUserIds(List.of(userId))
                .stream()
                .findFirst()
                .map(CountResultDto::count)
                .orElse(0L);

        return AdmMemberDetailResponse.of(member, postCount, commentCount);
    }

    /*
     * =========================
     * 내부 조회 로직
     * =========================
     */

    private Page<Member> findMembers(AdmMemberListRequest request, Pageable pageable) {

        String keyword = request.keyword();
        MemberStatus status = request.status();

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null;

        if (!hasKeyword && !hasStatus) {
            return memberRepository.findAll(pageable);
        }

        if (hasKeyword && !hasStatus) {
            return memberRepository.findByNicknameContainingOrEmailContaining(keyword, keyword, pageable);
        }

        if (!hasKeyword) {
            return memberRepository.findByStatus(status, pageable);
        }

        return memberRepository.findByStatusAndNicknameContainingOrStatusAndEmailContaining(
                status, keyword,
                status, keyword,
                pageable
        );
    }

    private Member findMemberOrThrow(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /*
     * =========================
     * 검증 로직
     * =========================
     */

    private void validateActiveMember(Member member) {

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.getStatus() == MemberStatus.BLACKLISTED) {
            throw new ApiException(AuthErrorCode.MEMBER_BLACKLISTED);
        }
    }

    private void validateUpdatableMember(Member member) {

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}