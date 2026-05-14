package com.back.devc.domain.member.member.controller;

import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse;
import com.back.devc.domain.member.member.dto.AdmMemberListRequest;
import com.back.devc.domain.member.member.dto.AdmMemberListResponse;
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.service.AdmMemberService;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.MemberSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdmMemberController {

    private final AdmMemberService adminMemberService;

    // 1. 회원 목록
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<AdmMemberListResponse>>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberStatus status
    ) {

        AdmMemberListRequest request = AdmMemberListRequest.builder()
                .page(page)
                .size(size)
                .keyword(keyword)
                .status(status)
                .build();

        Page<AdmMemberListResponse> response =
                adminMemberService.getMembers(request);

        MemberSuccessCode successCode = MemberSuccessCode.ADMIN_MEMBER_LIST_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    // 2. 상세 조회
    @GetMapping("/{userId}")
    public ResponseEntity<SuccessResponse<AdmMemberDetailResponse>> getMemberDetail(
            @PathVariable Long userId
    ) {

        AdmMemberDetailResponse response =
                adminMemberService.getMemberDetail(userId);

        MemberSuccessCode successCode = MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    // 3. 상태 변경
    @PatchMapping("/{userId}/status")
    public ResponseEntity<SuccessResponse<AdmMemberDetailResponse>> updateMemberStatus(
            @PathVariable Long userId,
            @RequestBody @Valid AdmMemberStatusUpdateRequest request
    ) {

        AdmMemberDetailResponse response =
                adminMemberService.updateMemberStatus(userId, request);

        MemberSuccessCode successCode = MemberSuccessCode.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }
}