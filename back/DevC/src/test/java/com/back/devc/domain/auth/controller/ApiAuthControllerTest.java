package com.back.devc.domain.auth.controller;

import com.back.devc.domain.member.member.controller.MemberController;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ApiAuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 회원가입() throws Exception {
        String email = "new-user@test.com";
        String password = "password123!";
        String nickname = "newUser";

        ResultActions resultActions = mvc.perform(
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
                .andDo(print());

        resultActions
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

        Member savedMember = memberRepository.findByEmail(email).orElseThrow();
        assertThat(savedMember.getNickname()).isEqualTo(nickname);
        assertThat(savedMember.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedMember.getPasswordHash())).isTrue();
    }

    @Test
    void 로그인() throws Exception {
        String email = "login-user@test.com";
        String rawPassword = "password123!";
        String nickname = "loginUser";

        Member member = Member.createLocalMember(email, passwordEncoder.encode(rawPassword), nickname);
        memberRepository.save(member);

        ResultActions resultActions = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void 로그아웃() throws Exception {
        ResultActions resultActions = mvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.message").isNotEmpty());
    }

    @Test
    void 비로그인_상태_확인() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));
    }

    @Test
    void 내정보_조회() throws Exception {
        String email = "me-user@test.com";
        String rawPassword = "password123!";
        String nickname = "meUser";

        Member member = Member.createLocalMember(email, passwordEncoder.encode(rawPassword), nickname);
        memberRepository.save(member);

        String loginResponse = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.data.accessToken");

        mvc.perform(
                        get("/api/users/me")
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
