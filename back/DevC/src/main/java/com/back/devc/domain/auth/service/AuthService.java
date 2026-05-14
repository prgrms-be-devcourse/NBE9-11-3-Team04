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
        log.info("로그인 시작 - email={}", request.email());
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 이메일 없음, email={}", request.email());
                    return new ApiException(AuthErrorCode.EMAIL_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치, email={}, userId={}", request.email(), member.getUserId());
            throw new ApiException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        if (member.getStatus() == MemberStatus.BLACKLISTED) {
            log.warn("로그인 실패 - 블랙리스트 회원, email={}, userId={}", request.email(), member.getUserId());
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
        Member savedMember = memberRepository.save(member);
        log.info("회원가입 완료 - userId={}, email={}", savedMember.getUserId(), savedMember.getEmail());

        return new SignUpResponse(
                savedMember.getUserId(),
                savedMember.getEmail(),
                savedMember.getNickname(),
                savedMember.getRole(),
                savedMember.getStatus()
        );
    }
}
