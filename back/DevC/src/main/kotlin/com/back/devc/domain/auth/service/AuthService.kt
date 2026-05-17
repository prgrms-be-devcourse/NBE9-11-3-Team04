package com.back.devc.domain.auth.service

import com.back.devc.domain.auth.dto.login.LoginRequest
import com.back.devc.domain.auth.dto.login.LoginResponse
import com.back.devc.domain.auth.dto.logout.LogoutResponse
import com.back.devc.domain.auth.dto.signup.SignUpRequest
import com.back.devc.domain.auth.dto.signup.SignUpResponse
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.security.jwt.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    // 로그아웃 성공 응답 DTO를 생성한다. 서버 상태 변경은 없다.
    @Transactional(readOnly = true)
    fun logout(): LogoutResponse {
        log.info("로그아웃 처리 - 서버 상태 변경 없음")
        return LogoutResponse.success()
    }

    // 사용자 인증 정보를 검증하고 JWT를 발급해 로그인 응답 DTO를 반환한다.
    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        log.info("로그인 시작 - email={}", request.email)

        val member = memberRepository.findByEmail(request.email)
            .orElseThrow {
                log.warn("로그인 실패 - 이메일 없음, email={}", request.email)
                ApiException(AuthErrorCode.EMAIL_NOT_FOUND)
            }

        if (!passwordEncoder.matches(request.password, member.passwordHash)) {
            log.warn("로그인 실패 - 비밀번호 불일치, email={}, userId={}", request.email, member.userId)
            throw ApiException(AuthErrorCode.PASSWORD_MISMATCH)
        }

        if (member.status == MemberStatus.BLACKLISTED) {
            log.warn("로그인 실패 - 블랙리스트 회원, email={}, userId={}", request.email, member.userId)
            throw ApiException(AuthErrorCode.MEMBER_BLACKLISTED)
        }

        val userId = member.userId ?: throw ApiException(AuthErrorCode.UNAUTHORIZED)
        val accessToken = jwtProvider.createAccessToken(member)

        log.info("로그인 완료 - userId={}, email={}", userId, member.email)

        return LoginResponse(
            userId = userId,
            email = member.email,
            nickname = member.nickname,
            role = member.role,
            status = member.status,
            accessToken = accessToken
        )
    }

    // 이메일/닉네임 중복을 검사한 뒤 로컬 회원을 생성하고 회원가입 응답 DTO를 반환한다.
    @Transactional
    fun signUp(request: SignUpRequest): SignUpResponse {
        log.info("회원가입 시작 - email={}, nickname={}", request.email, request.nickname)

        if (memberRepository.existsByEmail(request.email)) {
            log.warn("회원가입 실패 - 이메일 중복, email={}", request.email)
            throw ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        if (memberRepository.existsByNickname(request.nickname)) {
            log.warn("회원가입 실패 - 닉네임 중복, nickname={}", request.nickname)
            throw ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        val encodedPassword = passwordEncoder.encode(request.password)
            ?: throw ApiException(AuthErrorCode.BAD_REQUEST)

        val member = Member.createLocalMember(
            request.email,
            encodedPassword,
            request.nickname
        )

        try {
            val savedMember = memberRepository.saveAndFlush(member)
            val userId = savedMember.userId ?: throw ApiException(AuthErrorCode.UNAUTHORIZED)

            log.info("회원가입 완료 - userId={}, email={}", userId, savedMember.email)

            return SignUpResponse(
                userId = userId,
                email = savedMember.email,
                nickname = savedMember.nickname,
                role = savedMember.role,
                status = savedMember.status
            )
        } catch (e: DataIntegrityViolationException) {
            throw convertSignUpDuplicateException(request, e)
        }
    }

    // DataIntegrityViolationException에서 이메일/닉네임 중복 여부를 분석하여 적절한 ApiException으로 변환한다.
    private fun convertSignUpDuplicateException(
        request: SignUpRequest,
        e: DataIntegrityViolationException
    ): ApiException {
        val message = e.mostSpecificCause.message

        if (message != null) {
            val lowerMessage = message.lowercase()

            if (lowerMessage.contains("uk_users_email")) {
                log.warn("회원가입 실패 - DB 이메일 unique 제약 조건 위반, email={}", request.email)
                return ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
            }

            if (lowerMessage.contains("uk_users_nickname")) {
                log.warn("회원가입 실패 - DB 닉네임 unique 제약 조건 위반, nickname={}", request.nickname)
                return ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS)
            }
        }

        if (memberRepository.existsByEmail(request.email)) {
            log.warn("회원가입 실패 - DB 저장 후 이메일 중복 확인, email={}", request.email)
            return ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        if (memberRepository.existsByNickname(request.nickname)) {
            log.warn("회원가입 실패 - DB 저장 후 닉네임 중복 확인, nickname={}", request.nickname)
            return ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        throw e
    }
}