package com.back.devc.domain.auth.service;

import com.back.devc.domain.auth.dto.login.LoginResponse;
import com.back.devc.domain.auth.dto.oauth.OAuthPendingSignup;
import com.back.devc.domain.member.member.entity.AuthProvider;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import com.back.devc.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2MemberService {

    private static final String GITHUB_EMAIL_DOMAIN = "@users.noreply.github.com";
    private static final String KAKAO_EMAIL_DOMAIN = "@users.noreply.kakao.com";
    private static final String GOOGLE_EMAIL_DOMAIN = "@users.noreply.google.com";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuthLoginCodeService oAuthLoginCodeService;
    private final JwtProvider jwtProvider;

    // OAuth 로그인 코드를 소비해 사용자 인증 후 로그인 응답 DTO를 반환한다.
    @Transactional(readOnly = true)
    public LoginResponse exchangeLoginCode(String code) {
        Long userId = oAuthLoginCodeService.consume(code)
                .orElseThrow(() -> new ApiException(AuthErrorCode.UNAUTHORIZED));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus() == MemberStatus.BLACKLISTED) {
            throw new ApiException(AuthErrorCode.MEMBER_BLACKLISTED);
        }

        return toLoginResponse(member);
    }

    // OAuth2 회원가입 완료 후 JWT를 발급해 로그인 응답 DTO를 반환한다.
    @Transactional
    public LoginResponse completeSignupAndIssueToken(OAuthPendingSignup pending, String nickname) {
        Member member = completeSignup(pending, nickname);
        return toLoginResponse(member);
    }

    // OAuth2User의 속성에서 provider 정책에 맞춰 pendingSignup DTO를 생성한다.
    public OAuthPendingSignup buildPendingSignup(String provider, OAuth2User oauth2User) {
        return switch (toAuthProvider(provider)) {
            case GITHUB -> buildGithubPendingSignup(oauth2User);
            case KAKAO -> buildKakaoPendingSignup(oauth2User);
            case GOOGLE -> buildGooglePendingSignup(oauth2User);
            default -> throw new ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
        };
    }

    // provider/providerUserId로 기존 회원 존재 여부를 조회한다.
    public Optional<Member> findMemberByProviderUserId(String provider, String providerUserId) {
        AuthProvider authProvider = toAuthProvider(provider);
        return memberRepository.findByProviderAndProviderUserId(authProvider, providerUserId);
    }

    // GitHub OAuth 속성에서 pendingSignup DTO를 생성한다
    public OAuthPendingSignup buildGithubPendingSignup(OAuth2User oauth2User) {
        String providerUserId = valueAsString(oauth2User.getAttribute("id")).trim();
        if (providerUserId.isBlank()) {
            throw new ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING);
        }

        String login = valueAsString(oauth2User.getAttribute("login")).trim();
        String email = valueAsString(oauth2User.getAttribute("email")).trim();

        return new OAuthPendingSignup("github", providerUserId, email, login);
    }

    // Kakao OAuth 속성에서 pendingSignup DTO를 생성한다.
    public OAuthPendingSignup buildKakaoPendingSignup(OAuth2User oauth2User) {
        String providerUserId = valueAsString(oauth2User.getAttribute("id")).trim();
        if (providerUserId.isBlank()) {
            throw new ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING);
        }

        String email = "";
        String login = "";

        Object accountRaw = oauth2User.getAttribute("kakao_account");
        if (accountRaw instanceof Map<?, ?> accountMap) {
            email = valueAsString(accountMap.get("email")).trim();

            Object profileRaw = accountMap.get("profile");
            if (profileRaw instanceof Map<?, ?> profileMap) {
                login = valueAsString(profileMap.get("nickname")).trim();
            }
        }

        return new OAuthPendingSignup("kakao", providerUserId, email, login);
    }

    // OAuth 회원가입 요청을 검증하고 provider 정책에 맞춰 회원가입을 완료한다.
    public OAuthPendingSignup buildGooglePendingSignup(OAuth2User oauth2User) {
        String providerUserId = valueAsString(oauth2User.getAttribute("sub")).trim();
        if (providerUserId.isBlank()) {
            throw new ApiException(AuthErrorCode.OAUTH2_PROVIDER_USER_ID_MISSING);
        }

        String email = valueAsString(oauth2User.getAttribute("email")).trim();
        String login = valueAsString(oauth2User.getAttribute("name")).trim();

        return new OAuthPendingSignup("google", providerUserId, email, login);
    }

    @Transactional
    public Member completeSignup(OAuthPendingSignup pending, String nickname) {
        if (pending == null || pending.providerUserId() == null || pending.providerUserId().isBlank()) {
            throw new ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED);
        }

        AuthProvider provider = toAuthProvider(pending.provider());
        ProviderSpec spec = providerSpec(provider);

        return completeSignupByProvider(
                provider,
                pending,
                nickname,
                spec.fallbackEmailDomain(),
                spec.localPrefix()
        );
    }

    @Transactional
    public Member completeGithubSignup(OAuthPendingSignup pending, String nickname) {
        return completeSignupByProvider(AuthProvider.GITHUB, pending, nickname, GITHUB_EMAIL_DOMAIN, "github_");
    }

    @Transactional
    public Member completeKakaoSignup(OAuthPendingSignup pending, String nickname) {
        return completeSignupByProvider(AuthProvider.KAKAO, pending, nickname, KAKAO_EMAIL_DOMAIN, "kakao_");
    }

    @Transactional
    public Member completeGoogleSignup(OAuthPendingSignup pending, String nickname) {
        return completeSignupByProvider(AuthProvider.GOOGLE, pending, nickname, GOOGLE_EMAIL_DOMAIN, "google_");
    }

    private Member completeSignupByProvider(
            AuthProvider provider,
            OAuthPendingSignup pending,
            String nickname,
            String fallbackEmailDomain,
            String localPrefix
    ) {
        if (pending == null || pending.providerUserId() == null || pending.providerUserId().isBlank()) {
            throw new ApiException(AuthErrorCode.OAUTH2_PENDING_SIGNUP_REQUIRED);
        }

        Optional<Member> existing = memberRepository.findByProviderAndProviderUserId(
                provider, pending.providerUserId()
        );
        if (existing.isPresent()) {
            Member member = existing.get();

            if (member.getStatus() == MemberStatus.BLACKLISTED) {
                throw new ApiException(AuthErrorCode.MEMBER_BLACKLISTED);
            }

            return member;
        }

        String normalizedNickname = nickname == null ? "" : nickname.trim();
        if (normalizedNickname.isBlank()) {
            throw new ApiException(AuthErrorCode.BAD_REQUEST);
        }

        if (memberRepository.existsByNickname(normalizedNickname)) {
            throw new ApiException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        String resolvedEmail = resolveUniqueEmail(
                pending.emailFromProvider(),
                pending.providerUserId(),
                fallbackEmailDomain,
                localPrefix
        );
        String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        Member newMember = Member.createOAuthMember(
                provider,
                pending.providerUserId(),
                resolvedEmail,
                encodedPassword,
                normalizedNickname
        );

        return memberRepository.save(newMember);
    }

    private LoginResponse toLoginResponse(Member member) {
        String accessToken = jwtProvider.createAccessToken(member);

        return new LoginResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                accessToken
        );
    }

    private ProviderSpec providerSpec(AuthProvider provider) {
        return switch (provider) {
            case GITHUB -> new ProviderSpec(GITHUB_EMAIL_DOMAIN, "github_");
            case KAKAO -> new ProviderSpec(KAKAO_EMAIL_DOMAIN, "kakao_");
            case GOOGLE -> new ProviderSpec(GOOGLE_EMAIL_DOMAIN, "google_");
            default -> throw new ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
        };
    }

    // provider 문자열을 AuthProvider enum으로 변환한다. 지원하지 않는 provider인 경우 예외를 던진다.
    private AuthProvider toAuthProvider(String provider) {
        String normalized = normalizeProvider(provider);

        return switch (normalized) {
            case "github" -> AuthProvider.GITHUB;
            case "kakao" -> AuthProvider.KAKAO;
            case "google" -> AuthProvider.GOOGLE;
            default -> throw new ApiException(AuthErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
        };
    }

    // provider 문자열을 소문자로 정규화한다. null인 경우 빈 문자열로 처리한다.
    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    // provider에서 제공한 이메일이 유효하고 고유한 경우 그대로 사용한다. 그렇지 않으면 providerUserId와 fallbackDomain을 조합해 고유한 이메일을 생성한다.
    private String resolveUniqueEmail(
            String emailFromProvider,
            String providerUserId,
            String fallbackDomain,
            String localPrefix
    ) {
        if (emailFromProvider != null
                && !emailFromProvider.isBlank()
                && !memberRepository.existsByEmail(emailFromProvider)) {
            return emailFromProvider;
        }

        String baseLocalPart = localPrefix + providerUserId;
        String candidate = baseLocalPart + fallbackDomain;
        int sequence = 1;

        while (memberRepository.existsByEmail(candidate)) {
            candidate = baseLocalPart + "_" + sequence + fallbackDomain;
            sequence++;
        }

        return candidate;
    }

    // value가 null인 경우 빈 문자열을 반환하고, 그렇지 않으면 value.toString() 결과를 반환한다.
    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // 각 OAuth2 공급자별 이메일 생성 정책과 로컬 회원 식별자 접두어를 담는 간단한 레코드 클래스
    private record ProviderSpec(String fallbackEmailDomain, String localPrefix) {
    }
}
