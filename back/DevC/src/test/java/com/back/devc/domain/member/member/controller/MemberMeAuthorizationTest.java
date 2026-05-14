package com.back.devc.domain.member.member.controller;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
class MemberMeAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("내정보 조회 - 정상 사용자면 200 응답")
    void me_AuthorizedUser_Success() throws Exception {
        String email = "me-ok@test.com";
        String rawPassword = "password123!";
        String nickname = "meOkUser";

        Member member = Member.createLocalMember(email, passwordEncoder.encode(rawPassword), nickname);
        memberRepository.save(member);

        String accessToken = loginAndGetAccessToken(email, rawPassword);

        mvc.perform(
                        get("/api/users/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("내정보 조회 - 블랙리스트 사용자면 401 응답")
    void me_BlacklistedUser_Unauthorized() throws Exception {
        String email = "me-blacklisted@test.com";
        String rawPassword = "password123!";
        String nickname = "meBlockedUser";

        Member member = Member.createLocalMember(email, passwordEncoder.encode(rawPassword), nickname);
        Member savedMember = memberRepository.save(member);
        String accessToken = loginAndGetAccessToken(email, rawPassword);

        entityManager.createQuery("update Member m set m.status = :status where m.userId = :userId")
                .setParameter("status", MemberStatus.BLACKLISTED)
                .setParameter("userId", savedMember.getUserId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        mvc.perform(
                        get("/api/users/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));
    }

    @Test
    @DisplayName("내정보 조회 - 삭제된 사용자면 401 응답")
    void me_DeletedUser_Unauthorized() throws Exception {
        String email = "me-deleted@test.com";
        String rawPassword = "password123!";
        String nickname = "meDeletedUser";

        Member member = Member.createLocalMember(email, passwordEncoder.encode(rawPassword), nickname);
        Member savedMember = memberRepository.save(member);
        String accessToken = loginAndGetAccessToken(email, rawPassword);

        memberRepository.deleteById(savedMember.getUserId());
        memberRepository.flush();

        mvc.perform(
                        get("/api/users/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));
    }

    private String loginAndGetAccessToken(String email, String rawPassword) throws Exception {
        String responseBody = mvc.perform(
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

        return JsonPath.read(responseBody, "$.data.accessToken");
    }
}
