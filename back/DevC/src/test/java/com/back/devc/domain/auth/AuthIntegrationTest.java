package com.back.devc.domain.auth;

import com.back.devc.domain.auth.controller.AuthController;
import com.back.devc.domain.member.member.controller.MemberController;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("로그인/회원가입 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtProvider jwtProvider;

    @Value("${custom.jwt.secret-key}")
    private String jwtSecretKey;

    @Test
    @DisplayName("회원가입 후 로그인하고 내 정보를 조회한 뒤 로그아웃한다")
    void signUp_thenLogin_thenGetMe_thenLogout() throws Exception {
        // given
        String email = "auth-flow@test.com";
        String password = "password123!";
        String nickname = "authFlowUser";

        // when - 회원가입
        mvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s",
                                          "nickname": "%s"
                                        }
                                        """.formatted(email, password, nickname))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("signUp"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // then - DB에 회원이 저장되고 비밀번호가 암호화된다.
        Member savedMember = memberRepository.findByEmail(email).orElseThrow();

        assertThat(savedMember.getNickname()).isEqualTo(nickname);
        assertThat(savedMember.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedMember.getPasswordHash())).isTrue();

        // when - 로그인
        String loginResponseBody = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, password))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.userId").value(savedMember.getUserId()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(loginResponseBody, "$.data.accessToken");

        // when - 발급받은 accessToken으로 내 정보 조회
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.userId").value(savedMember.getUserId()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // when - 로그아웃
        mvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.message").isNotEmpty());
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 실패한다")
    void signUp_fail_whenEmailAlreadyExists() throws Exception {
        // given
        String email = "duplicate-email@test.com";
        String password = "password123!";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(password),
                "duplicateEmailUser"
        );
        memberRepository.saveAndFlush(member);

        // when & then
        mvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s",
                                          "nickname": "newNickname"
                                        }
                                        """.formatted(email, password))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("signUp"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_EMAIL"))
                .andExpect(jsonPath("$.timestamp").exists());

        long savedMemberCount = memberRepository.findAll()
                .stream()
                .filter(saved -> email.equals(saved.getEmail()))
                .count();

        assertThat(savedMemberCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 가입된 닉네임으로 회원가입하면 실패한다")
    void signUp_fail_whenNicknameAlreadyExists() throws Exception {
        // given
        String nickname = "duplicateNicknameUser";
        String password = "password123!";

        Member member = Member.createLocalMember(
                "duplicate-nickname@test.com",
                passwordEncoder.encode(password),
                nickname
        );
        memberRepository.saveAndFlush(member);

        // when & then
        mvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "new-email-for-nickname@test.com",
                                          "password": "%s",
                                          "nickname": "%s"
                                        }
                                        """.formatted(password, nickname))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("signUp"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_NICKNAME"))
                .andExpect(jsonPath("$.timestamp").exists());

        long savedMemberCount = memberRepository.findAll()
                .stream()
                .filter(saved -> nickname.equals(saved.getNickname()))
                .count();

        assertThat(savedMemberCount).isEqualTo(1);
    }

    @Test
    @DisplayName("잘못된 회원가입 요청값이면 실패한다")
    void signUp_fail_whenRequestInvalid() throws Exception {
        // when & then
        mvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "",
                                          "password": "",
                                          "nickname": ""
                                        }
                                        """)
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("signUp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validation").exists());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 실패한다")
    void login_fail_whenEmailNotFound() throws Exception {
        // when & then
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "not-found-auth@test.com",
                                          "password": "password123!"
                                        }
                                        """)
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUTH_404_EMAIL_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void login_fail_whenPasswordMismatch() throws Exception {
        // given
        String email = "password-mismatch@test.com";
        String rawPassword = "password123!";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                "passwordMismatchUser"
        );
        memberRepository.saveAndFlush(member);

        // when & then
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "wrongPassword123!"
                                        }
                                        """.formatted(email))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("블랙리스트 회원은 로그인할 수 없다")
    void login_fail_whenMemberBlacklisted() throws Exception {
        // given
        String email = "blacklisted-auth@test.com";
        String rawPassword = "password123!";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                "blacklistedAuthUser"
        );
        member.updateStatus(MemberStatus.BLACKLISTED);
        memberRepository.saveAndFlush(member);

        // when & then
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403_MEMBER_BLACKLISTED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("삭제된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    void me_fail_whenMemberDeletedAfterLogin() throws Exception {
        // given
        String email = "deleted-after-login@test.com";
        String rawPassword = "password123!";
        String nickname = "deletedAfterLoginUser";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                nickname
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String loginResponseBody = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(loginResponseBody, "$.data.accessToken");

        memberRepository.deleteById(savedMember.getUserId());
        memberRepository.flush();
        entityManager.clear();

        // when & then
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("블랙리스트 처리된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    void me_fail_whenMemberBlacklistedAfterLogin() throws Exception {
        // given
        String email = "blacklisted-after-login@test.com";
        String rawPassword = "password123!";
        String nickname = "blacklistedAfterLoginUser";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                nickname
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String loginResponseBody = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(loginResponseBody, "$.data.accessToken");

        entityManager.createQuery("update Member m set m.status = :status where m.userId = :userId")
                .setParameter("status", MemberStatus.BLACKLISTED)
                .setParameter("userId", savedMember.getUserId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // when & then
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("비로그인 사용자는 내 정보를 조회할 수 없다")
    void me_fail_whenAccessTokenMissing() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 accessToken으로 내 정보를 조회할 수 없다")
    void withdraw_thenMeFailsWithExistingAccessToken() throws Exception {
        // given
        String email = "withdraw-auth@test.com";
        String rawPassword = "password123!";
        String nickname = "withdrawAuthUser";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                nickname
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String loginResponseBody = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(loginResponseBody, "$.data.accessToken");

        // when - 인증된 사용자가 회원 탈퇴 API를 호출한다.
        mvc.perform(
                        delete("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(savedMember.getUserId()));

        entityManager.flush();
        entityManager.clear();

        // then - 탈퇴 이후 기존 토큰으로는 보호 API에 접근할 수 없다.
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("만료된 JWT로 내 정보를 조회할 수 없다")
    void me_fail_whenAccessTokenExpired() throws Exception {
        // given
        Member member = Member.createLocalMember(
                "expired-token@test.com",
                passwordEncoder.encode("password123!"),
                "expiredTokenUser"
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String expiredToken = createExpiredAccessToken(savedMember);

        // when & then
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("변조된 JWT로 내 정보를 조회할 수 없다")
    void me_fail_whenAccessTokenTampered() throws Exception {
        // given
        Member member = Member.createLocalMember(
                "tampered-token@test.com",
                passwordEncoder.encode("password123!"),
                "tamperedTokenUser"
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String validToken = jwtProvider.createAccessToken(savedMember);
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "x";

        // when & then
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Authorization 헤더 형식이 잘못되면 내 정보를 조회할 수 없다")
    void me_fail_whenAuthorizationHeaderInvalid() throws Exception {
        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
                .andExpect(jsonPath("$.timestamp").exists());

        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Token invalid-token")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());

        mvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "invalid-token")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("로그아웃하면 access_token 쿠키가 정확히 만료된다")
    void logout_expiresAccessTokenCookieStrictly() throws Exception {
        // when & then
        mvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=")));
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 이메일과 닉네임으로 다시 가입할 수 있다")
    void signUp_success_afterWithdrawWithSameEmailAndNickname() throws Exception {
        // given
        String email = "resignup-after-withdraw@test.com";
        String password = "password123!";
        String nickname = "resignupAfterWithdrawUser";

        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(password),
                nickname
        );
        Member savedMember = memberRepository.saveAndFlush(member);

        String accessToken = jwtProvider.createAccessToken(savedMember);

        mvc.perform(
                        delete("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(savedMember.getUserId()));

        entityManager.flush();
        entityManager.clear();

        // when & then
        mvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s",
                                          "nickname": "%s"
                                        }
                                        """.formatted(email, password, nickname))
                )
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("signUp"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private String createExpiredAccessToken(Member member) {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        Instant expiredAt = Instant.now().minusSeconds(3600);

        SecretKey secretKey = Keys.hmacShaKeyFor(
                jwtSecretKey.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .subject(String.valueOf(member.getUserId()))
                .claim("tokenType", "ACCESS")
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiredAt))
                .signWith(secretKey)
                .compact();
    }
}