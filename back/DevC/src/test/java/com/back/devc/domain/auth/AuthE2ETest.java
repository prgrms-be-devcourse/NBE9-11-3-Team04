package com.back.devc.domain.auth;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("인증 E2E 테스트")
class AuthE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // E2E 테스트가 기존 테스트 데이터에 의존하지 않도록 관련 테이블을 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS");
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS");
        jdbcTemplate.update("DELETE FROM COMMENTS");
        jdbcTemplate.update("DELETE FROM POST_LIKES");
        jdbcTemplate.update("DELETE FROM BOOKMARKS");
        jdbcTemplate.update("DELETE FROM REPORTS");
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS");
        jdbcTemplate.update("DELETE FROM POST");
        jdbcTemplate.update("DELETE FROM USERS");
    }

    @Test
    @DisplayName("회원가입 후 로그인하고 내 정보를 조회한 뒤 로그아웃할 수 있다")
    void signup_thenLogin_thenGetMe_thenLogout() throws Exception {
        // given
        String email = "auth-flow@test.com";
        String password = "password123!";
        String nickname = "authFlowUser";

        // 1. 사용자가 회원가입 API를 호출한다.
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(email, password, nickname)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        Long userId = extractLong(signupResult, "userId");

        // 2. 회원가입한 사용자가 DB에 저장됐고 비밀번호가 암호화됐는지 확인한다.
        Member savedMember = memberRepository.findByEmail(email).orElseThrow();

        assertThat(savedMember.getUserId()).isEqualTo(userId);
        assertThat(savedMember.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedMember.getPasswordHash())).isTrue();

        // 3. 회원가입한 계정으로 로그인 API를 호출한다.
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = extractString(loginResult, "accessToken");

        // 4. 발급받은 accessToken으로 내 정보 조회 API를 호출한다.
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 5. 로그아웃 API를 호출한다.
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
                .andExpect(jsonPath("$.data.message").isNotEmpty());
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 실패한다")
    void signup_fail_whenEmailAlreadyExists() throws Exception {
        // given
        String email = "duplicate-email@test.com";
        String password = "password123!";

        saveLocalMember(email, password, "duplicateEmailUser", MemberStatus.ACTIVE);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "newNickname"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_EMAIL"));

        long count = memberRepository.findAll()
                .stream()
                .filter(member -> email.equals(member.getEmail()))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 가입된 닉네임으로 회원가입하면 실패한다")
    void signup_fail_whenNicknameAlreadyExists() throws Exception {
        // given
        String nickname = "duplicateNicknameUser";
        String password = "password123!";

        saveLocalMember("duplicate-nickname@test.com", password, nickname, MemberStatus.ACTIVE);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new-email-for-nickname@test.com",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(password, nickname)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_NICKNAME"));

        long count = memberRepository.findAll()
                .stream()
                .filter(member -> nickname.equals(member.getNickname()))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("잘못된 회원가입 요청값이면 실패한다")
    void signup_fail_whenRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": "",
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.validation").exists());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 실패한다")
    void login_fail_whenEmailNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-found-auth@test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUTH_404_EMAIL_NOT_FOUND"));
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void login_fail_whenPasswordMismatch() throws Exception {
        // given
        String email = "password-mismatch@test.com";
        String password = "password123!";

        saveLocalMember(email, password, "passwordMismatchUser", MemberStatus.ACTIVE);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrongPassword123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("블랙리스트 회원은 로그인할 수 없다")
    void login_fail_whenMemberBlacklisted() throws Exception {
        // given
        String email = "blacklisted-auth@test.com";
        String password = "password123!";

        saveLocalMember(email, password, "blacklistedAuthUser", MemberStatus.BLACKLISTED);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403_MEMBER_BLACKLISTED"));
    }

    @Test
    @DisplayName("비로그인 사용자는 내 정보를 조회할 수 없다")
    void me_fail_whenAccessTokenMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"));
    }

    @Test
    @DisplayName("삭제된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    void me_fail_whenMemberDeletedAfterLogin() throws Exception {
        // given
        String email = "deleted-after-login@test.com";
        String password = "password123!";

        Member member = saveLocalMember(email, password, "deletedAfterLoginUser", MemberStatus.ACTIVE);
        String accessToken = loginAndExtractAccessToken(email, password);

        // when
        memberRepository.deleteById(member.getUserId());
        memberRepository.flush();

        // then
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("블랙리스트 처리된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    void me_fail_whenMemberBlacklistedAfterLogin() throws Exception {
        // given
        String email = "blacklisted-after-login@test.com";
        String password = "password123!";

        Member member = saveLocalMember(email, password, "blacklistedAfterLoginUser", MemberStatus.ACTIVE);
        String accessToken = loginAndExtractAccessToken(email, password);

        // when
        member.updateStatus(MemberStatus.BLACKLISTED);
        memberRepository.saveAndFlush(member);

        // then
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 accessToken으로 내 정보를 조회할 수 없다")
    void withdraw_thenMeFailsWithExistingAccessToken() throws Exception {
        // given
        String email = "withdraw-auth@test.com";
        String password = "password123!";

        Member member = saveLocalMember(email, password, "withdrawAuthUser", MemberStatus.ACTIVE);
        String accessToken = loginAndExtractAccessToken(email, password);

        // when
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(member.getUserId()));

        // then
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"));
    }

    private Member saveLocalMember(
            String email,
            String rawPassword,
            String nickname,
            MemberStatus status
    ) {
        Member member = Member.createLocalMember(
                email,
                passwordEncoder.encode(rawPassword),
                nickname
        );
        member.updateStatus(status);

        return memberRepository.saveAndFlush(member);
    }

    @SuppressWarnings("unchecked")
    private String loginAndExtractAccessToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        Map<String, Object> responseBody = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                Map.class
        );
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        return String.valueOf(data.get("accessToken"));
    }

    @SuppressWarnings("unchecked")
    private Long extractLong(MvcResult result, String fieldName) throws Exception {
        Map<String, Object> responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        return toLong(data.get(fieldName));
    }

    @SuppressWarnings("unchecked")
    private String extractString(MvcResult result, String fieldName) throws Exception {
        Map<String, Object> responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        return String.valueOf(data.get(fieldName));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}