package com.back.devc.domain.auth.service

import com.back.devc.domain.auth.dto.login.LoginResponse
import com.back.devc.domain.auth.dto.oauth.OAuthPendingSignup
import com.back.devc.domain.member.member.entity.AuthProvider
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.security.jwt.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OAuth2MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val oAuthLoginCodeService: OAuthLoginCodeService,
    private val jwtProvider: JwtProvider
) {

    companion object {
        private const val GITHUB_EMAIL_DOMAIN = "@users.noreply.github.com"
        private const val KAKAO_EMAIL_DOMAIN = "@users.noreply.kakao.com"
        private const val GOOGLE_EMAIL_DOMAIN = "@users.noreply.google.com"
    }

    // OAuth 로그인 코드를 소비해 사용자 인증 후 로그인 응답 DTO를 반환한다.
    @Transactional(readOnly = true)
    fun exchangeLoginCode(code: String): LoginResponse {
        val userId = oAuthLoginCodeService.consume(code)
            .orElseThrow { ApiException(AuthErrorCode.UNAUTHORIZED) }

        val member = memberRepository.findById(userId)
            .orElseThrow { ApiException(MemberErrorCode.MEMBER_NOT_FOUND) }

        if (member.status == MemberStatus.BLACKLISTED) {
            throw ApiException(AuthErrorCode.MEMBER_BLACKLISTED)
        }

        return toLoginResponse(member)
    }

    // OAuth2 회원가입 완료 후 JWT를 발급한 로그인 응답 DTO를 반환한다.
    @Transactional
    fun completeSignupAndIssueToken(
        pending: OAuthPendingSignup?,
        nickname: String?
    ): LoginResponse {
        val member = completeSignup(pending, nickname)
        return toLoginResponse(member)
    }

    // OAuth2User의 속성에서 provider 정책에 맞는 pendingSignup DTO를 생성한다.
    fun buildPendingSignup(
        provider: String?,
        oauth2User: OAuth2User
    ): OAuthPendingSignup {
        return when (toAuthProvider(provider)) {
            AuthProvider.GITHUB -> buildGithubPendingSignup(oauth2User)
            AuthProvider.KAKAO -> buildKakaoPendingSignup(oauth2User)
            AuthProvider.GOOGLE -> buildGooglePendingSignup(oauth2User)
            AuthProvider.LOCAL -> throw ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER)
        }
    }

    // provider/providerUserId로 기존 회원 존재 여부를 조회한다.
    fun findMemberByProviderUserId(
        provider: String?,
        providerUserId: String
    ): Optional<Member> {
        val authProvider = toAuthProvider(provider)
        return memberRepository.findByProviderAndProviderUserId(authProvider, providerUserId)
    }

    // GitHub OAuth 속성에서 pendingSignup DTO를 생성한다.
    fun buildGithubPendingSignup(oauth2User: OAuth2User): OAuthPendingSignup {
        val providerUserId = valueAsString(oauth2User.getAttribute<Any>("id")).trim()
        if (providerUserId.isBlank()) {
            throw ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING)
        }

        val login = valueAsString(oauth2User.getAttribute<Any>("login")).trim()
        val email = valueAsString(oauth2User.getAttribute<Any>("email")).trim()

        return OAuthPendingSignup("github", providerUserId, email, login)
    }

    // Kakao OAuth 속성에서 pendingSignup DTO를 생성한다.
    fun buildKakaoPendingSignup(oauth2User: OAuth2User): OAuthPendingSignup {
        val providerUserId = valueAsString(oauth2User.getAttribute<Any>("id")).trim()
        if (providerUserId.isBlank()) {
            throw ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING)
        }

        val account = oauth2User.getAttribute<Any>("kakao_account") as? Map<*, *>
        val profile = account?.get("profile") as? Map<*, *>

        val email = valueAsString(account?.get("email")).trim()
        val login = valueAsString(profile?.get("nickname")).trim()

        return OAuthPendingSignup("kakao", providerUserId, email, login)
    }

    // Google OAuth 속성에서 pendingSignup DTO를 생성한다.
    fun buildGooglePendingSignup(oauth2User: OAuth2User): OAuthPendingSignup {
        val providerUserId = valueAsString(oauth2User.getAttribute<Any>("sub")).trim()
        if (providerUserId.isBlank()) {
            throw ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING)
        }

        val email = valueAsString(oauth2User.getAttribute<Any>("email")).trim()
        val login = valueAsString(oauth2User.getAttribute<Any>("name")).trim()

        return OAuthPendingSignup("google", providerUserId, email, login)
    }

    @Transactional
    fun completeSignup(
        pending: OAuthPendingSignup?,
        nickname: String?
    ): Member {
        if (pending == null || pending.providerUserId.isBlank()) {
            throw ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED)
        }

        val provider = toAuthProvider(pending.provider)
        val spec = providerSpec(provider)

        return completeSignupByProvider(
            provider = provider,
            pending = pending,
            nickname = nickname,
            fallbackEmailDomain = spec.fallbackEmailDomain,
            localPrefix = spec.localPrefix
        )
    }

    @Transactional
    fun completeGithubSignup(
        pending: OAuthPendingSignup?,
        nickname: String?
    ): Member {
        return completeSignupByProvider(
            provider = AuthProvider.GITHUB,
            pending = pending,
            nickname = nickname,
            fallbackEmailDomain = GITHUB_EMAIL_DOMAIN,
            localPrefix = "github_"
        )
    }

    @Transactional
    fun completeKakaoSignup(
        pending: OAuthPendingSignup?,
        nickname: String?
    ): Member {
        return completeSignupByProvider(
            provider = AuthProvider.KAKAO,
            pending = pending,
            nickname = nickname,
            fallbackEmailDomain = KAKAO_EMAIL_DOMAIN,
            localPrefix = "kakao_"
        )
    }

    @Transactional
    fun completeGoogleSignup(
        pending: OAuthPendingSignup?,
        nickname: String?
    ): Member {
        return completeSignupByProvider(
            provider = AuthProvider.GOOGLE,
            pending = pending,
            nickname = nickname,
            fallbackEmailDomain = GOOGLE_EMAIL_DOMAIN,
            localPrefix = "google_"
        )
    }

    private fun completeSignupByProvider(
        provider: AuthProvider,
        pending: OAuthPendingSignup?,
        nickname: String?,
        fallbackEmailDomain: String,
        localPrefix: String
    ): Member {
        if (pending == null || pending.providerUserId.isBlank()) {
            throw ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED)
        }

        val existingMember = memberRepository
            .findByProviderAndProviderUserId(provider, pending.providerUserId)
            .orElse(null)

        if (existingMember != null) {
            if (existingMember.status == MemberStatus.BLACKLISTED) {
                throw ApiException(AuthErrorCode.MEMBER_BLACKLISTED)
            }

            return existingMember
        }

        val normalizedNickname = nickname?.trim().orEmpty()
        if (normalizedNickname.isBlank()) {
            throw ApiException(AuthErrorCode.BAD_REQUEST)
        }

        if (memberRepository.existsByNickname(normalizedNickname)) {
            throw ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        val resolvedEmail = resolveUniqueEmail(
            emailFromProvider = pending.emailFromProvider,
            providerUserId = pending.providerUserId,
            fallbackDomain = fallbackEmailDomain,
            localPrefix = localPrefix
        )

        val encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString())
            ?: throw ApiException(AuthErrorCode.BAD_REQUEST)

        val newMember = Member.createOAuthMember(
            provider,
            pending.providerUserId,
            resolvedEmail,
            encodedPassword,
            normalizedNickname
        )

        return memberRepository.save(newMember)
    }

    private fun toLoginResponse(member: Member): LoginResponse {
        val userId = member.userId ?: throw ApiException(AuthErrorCode.UNAUTHORIZED)
        val accessToken = jwtProvider.createAccessToken(member)

        return LoginResponse(
            userId = userId,
            email = member.email,
            nickname = member.nickname,
            role = member.role,
            status = member.status,
            accessToken = accessToken
        )
    }

    private fun providerSpec(provider: AuthProvider): ProviderSpec {
        return when (provider) {
            AuthProvider.GITHUB -> ProviderSpec(GITHUB_EMAIL_DOMAIN, "github_")
            AuthProvider.KAKAO -> ProviderSpec(KAKAO_EMAIL_DOMAIN, "kakao_")
            AuthProvider.GOOGLE -> ProviderSpec(GOOGLE_EMAIL_DOMAIN, "google_")
            AuthProvider.LOCAL -> throw ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER)
        }
    }

    private fun toAuthProvider(provider: String?): AuthProvider {
        return when (normalizeProvider(provider)) {
            "github" -> AuthProvider.GITHUB
            "kakao" -> AuthProvider.KAKAO
            "google" -> AuthProvider.GOOGLE
            else -> throw ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER)
        }
    }

    private fun normalizeProvider(provider: String?): String {
        return provider?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }

    private fun resolveUniqueEmail(
        emailFromProvider: String?,
        providerUserId: String,
        fallbackDomain: String,
        localPrefix: String
    ): String {
        if (!emailFromProvider.isNullOrBlank() &&
            !memberRepository.existsByEmail(emailFromProvider)
        ) {
            return emailFromProvider
        }

        val baseLocalPart = localPrefix + providerUserId
        var candidate = baseLocalPart + fallbackDomain
        var sequence = 1

        while (memberRepository.existsByEmail(candidate)) {
            candidate = "${baseLocalPart}_$sequence$fallbackDomain"
            sequence++
        }

        return candidate
    }

    private fun valueAsString(value: Any?): String {
        return value?.toString().orEmpty()
    }

    private data class ProviderSpec(
        val fallbackEmailDomain: String,
        val localPrefix: String
    )
}