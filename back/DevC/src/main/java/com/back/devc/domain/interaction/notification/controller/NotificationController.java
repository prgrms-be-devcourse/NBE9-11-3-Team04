package com.back.devc.domain.interaction.notification.controller;

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse;
import com.back.devc.domain.interaction.notification.dto.NotificationResponse;
import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.NotificationSuccessCode;

/**
 * 알림 조회/읽음 처리 API 컨트롤러
 *
 * 이 컨트롤러는 현재 로그인한 사용자를 기준으로만 알림을 조회하고 읽음 처리
 *
 * 별도 사용자 id를 파라미터로 받지 않는 이유
 * - 알림은 민감한 개인 데이터이므로, 프론트에서 userId를 넘겨받아 처리하면
 *   다른 사용자의 알림을 조회할 여지가 생길 수 있음
 * - 따라서 서버가 SecurityContext 안의 인증 정보만 보고 현재 로그인 사용자를 식별
 *
 * 이 프로젝트는 로그인 방식이 2가지
 * 1) 일반 이메일 로그인: JwtPrincipal 사용
 * 2) OAuth 로그인: OAuth2User 사용
 *
 * 그래서 아래 helper 메서드(getAuthenticatedUserId)는
 * 인증 주체 타입에 따라 현재 회원의 userId를 얻을 수 있게 분기 처리
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    // 실제 알림 조회/읽음 처리 비즈니스 로직은 service 계층에서 수행
    private final NotificationService notificationService;
    // OAuth 로그인 사용자는 JwtPrincipal이 없을 수 있어서,
    // email 기반으로 Member를 다시 찾아 userId를 얻을 때 사용
    private final MemberRepository memberRepository;

    /**
     * 현재 로그인한 사용자의 알림 목록 조회
     *
     * - 일반 로그인 사용자는 JwtPrincipal에서 userId를 꺼냄
     * - OAuth 로그인 사용자는 OAuth2User에서 email을 꺼낸 뒤 Member를 조회해서 userId를 얻음
     */
    @GetMapping
    public ResponseEntity<SuccessResponse<NotificationListResponse>> getMyNotifications(Authentication authentication) {
        NotificationListResponse response = notificationService.getMyNotifications(getAuthenticatedUserId(authentication));
        NotificationSuccessCode successCode = NotificationSuccessCode.NOTIFICATION_200_LIST;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    /**
     * 현재 로그인한 사용자의 특정 알림을 읽음 처리
     *
     * notificationId만 받아도 되는 이유는,
     * 실제 service 계층에서 "이 알림이 현재 로그인한 사용자의 알림인지"를 함께 검증하기 때문
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<SuccessResponse<NotificationResponse>> readNotification(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        NotificationResponse response = notificationService.readNotification(
                notificationId,
                getAuthenticatedUserId(authentication)
        );
        NotificationSuccessCode successCode = NotificationSuccessCode.NOTIFICATION_200_READ;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    /**
     * SecurityContext에 들어있는 인증 객체에서 현재 로그인 사용자의 userId를 추출
     *
     * 지원하는 인증 타입
     * - JwtPrincipal : 일반 로그인/JWT 인증
     * - OAuth2User   : OAuth 로그인 인증
     *
     * OAuth2User는 바로 userId를 들고 있지 않을 수 있으므로,
     * provider가 내려준 email로 우리 서비스의 Member를 다시 조회해서 userId를 얻음
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        // 인증 주체(principal) 타입에 따라 현재 회원 식별 방식이 달라짐
        Object principal = authentication.getPrincipal();
        // 일반 이메일 로그인/JWT 인증 사용자는 JwtPrincipal 안에 userId가 이미 들어 있음
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }
        // OAuth 로그인 사용자는 email을 기준으로 우리 서비스의 Member를 다시 조회해 userId를 얻음
        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");

            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAuth 사용자 정보를 확인할 수 없습니다.");
            }

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));

            return member.getUserId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }
}