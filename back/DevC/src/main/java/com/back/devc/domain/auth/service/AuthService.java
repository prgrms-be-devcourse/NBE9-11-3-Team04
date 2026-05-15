package com.back.devc.domain.auth.service;

import com.back.devc.domain.auth.dto.login.LoginRequest;
import com.back.devc.domain.auth.dto.login.LoginResponse;
import com.back.devc.domain.auth.dto.logout.LogoutResponse;
import com.back.devc.domain.auth.dto.signup.SignUpRequest;
import com.back.devc.domain.auth.dto.signup.SignUpResponse;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 로그아웃 성공 응답 DTO를 생성한다. (서버 상태 변경 없음)
    @Transactional(readOnly = true)
    public LogoutResponse logout() {
        log.info("로그아웃 처리 - 서버 상태 변경 없음");
        return LogoutResponse.success();
    }

    // 사용자 인증 정보를 검증하고 JWT를 발급해 로그인 응답 DTO를 반환한다.
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시작 - email={}", request.getEmail());
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 이메일 없음, email={}", request.getEmail());
                    return new ApiException(AuthErrorCode.EMAIL_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치, email={}, userId={}", request.getEmail(), member.getUserId());
            throw new ApiException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        if (member.getStatus() == MemberStatus.BLACKLISTED) {
            log.warn("로그인 실패 - 블랙리스트 회원, email={}, userId={}", request.getEmail(), member.getUserId());
            throw new ApiException(AuthErrorCode.MEMBER_BLACKLISTED);
        }

        String accessToken = jwtProvider.createAccessToken(member);
        log.info("로그인 완료 - userId={}, email={}", member.getUserId(), member.getEmail());

        return new LoginResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                accessToken
        );
    }

    // 이메일/닉네임 중복을 검사한 뒤 로컬 회원을 생성하고 회원가입 응답 DTO를 반환한다.
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        log.info("회원가입 시작 - email={}, nickname={}", request.email(), request.nickname());

        if (memberRepository.existsByEmail(request.email())) {
            log.warn("회원가입 실패 - 이메일 중복, email={}", request.email());
            throw new ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            log.warn("회원가입 실패 - 닉네임 중복, nickname={}", request.nickname());
            throw new ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = Member.createLocalMember(request.email(), encodedPassword, request.nickname());

        try {
            // unique 제약 조건 위반을 회원가입 메서드 안에서 바로 감지하기 위해 flush까지 수행
            Member savedMember = memberRepository.saveAndFlush(member);

            log.info("회원가입 완료 - userId={}, email={}", savedMember.getUserId(), savedMember.getEmail());

            return new SignUpResponse(
                    savedMember.getUserId(),
                    savedMember.getEmail(),
                    savedMember.getNickname(),
                    savedMember.getRole(),
                    savedMember.getStatus()
            );
        } catch (DataIntegrityViolationException e) {
            //  동시 회원가입 상황에서 DB unique 제약 조건 위반 시 기존 중복 예외로 변환
            throw convertSignUpDuplicateException(request, e);
        }
    }

    private ApiException convertSignUpDuplicateException(
            SignUpRequest request,
            DataIntegrityViolationException e
    ) {
        String message = e.getMostSpecificCause().getMessage();

        if (message != null) {
            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("uk_users_email")) {
                log.warn("회원가입 실패 - DB 이메일 unique 제약 조건 위반, email={}", request.email());
                return new ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
            }

            if (lowerMessage.contains("uk_users_nickname")) {
                log.warn("회원가입 실패 - DB 닉네임 unique 제약 조건 위반, nickname={}", request.nickname());
                return new ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
            }
        }

        // DB마다 제약 조건 위반 메시지가 다를 수 있으므로 실제 중복 여부를 한 번 더 확인
        if (memberRepository.existsByEmail(request.email())) {
            log.warn("회원가입 실패 - DB 저장 후 이메일 중복 확인, email={}", request.email());
            return new ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            log.warn("회원가입 실패 - DB 저장 후 닉네임 중복 확인, nickname={}", request.nickname());
            return new ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 이메일/닉네임 중복이 아닌 다른 DB 무결성 오류는 원래 예외 그대로 전파
        throw e;
    }
}
