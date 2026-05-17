package com.back.devc.domain.post.post.e2e;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("게시글 E2E 테스트")
class PostControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long userId;
    private Long otherUserId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        otherUserId = 2L;
        categoryId = 1L;

        jdbcTemplate.update("DELETE FROM POST");
        jdbcTemplate.update("DELETE FROM USERS");
        jdbcTemplate.update("DELETE FROM CATEGORY");

        // 게시글 작성자
        jdbcTemplate.update("""
                INSERT INTO USERS (
                    USER_ID,
                    CREATED_AT,
                    EMAIL,
                    NICKNAME,
                    PASSWORD_HASH,
                    PROVIDER,
                    PROVIDER_USER_ID,
                    ROLE,
                    STATUS,
                    SUSPENDED_UNTIL,
                    UPDATED_AT
                ) VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                userId,
                "post-user@test.com",
                "게시글작성자",
                passwordEncoder.encode("password123!"),
                "LOCAL",
                "post-user@test.com",
                "USER",
                "ACTIVE",
                null
        );

        // 다른 사용자
        jdbcTemplate.update("""
                INSERT INTO USERS (
                    USER_ID,
                    CREATED_AT,
                    EMAIL,
                    NICKNAME,
                    PASSWORD_HASH,
                    PROVIDER,
                    PROVIDER_USER_ID,
                    ROLE,
                    STATUS,
                    SUSPENDED_UNTIL,
                    UPDATED_AT
                ) VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                otherUserId,
                "other-user@test.com",
                "다른사용자",
                passwordEncoder.encode("password123!"),
                "LOCAL",
                "other-user@test.com",
                "USER",
                "ACTIVE",
                null
        );

        // 카테고리
        jdbcTemplate.update("""
                INSERT INTO CATEGORY (
                    CATEGORY_ID,
                    NAME
                ) VALUES (?, ?)
                """,
                categoryId,
                "자유게시판"
        );
    }

    @Test
    @DisplayName("사용자 게시글 작성 → 조회 → 수정 → 삭제 전체 시나리오")
    void createReadUpdateDeletePost() throws Exception {
        String accessToken = loginAndExtractAccessToken(
                "post-user@test.com",
                "password123!"
        );

        // 1. 게시글 작성
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E 테스트 제목",
                                  "content": "E2E 테스트 내용",
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("POST_201_CREATE_SUCCESS"))
                .andExpect(jsonPath("$.data.postId").exists())
                .andReturn();

        Long postId = extractLong(createResult, "postId");

        // 2. 게시글 상세 조회
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.title").value("E2E 테스트 제목"))
                .andExpect(jsonPath("$.data.content").value("E2E 테스트 내용"));

        // 3. 게시글 수정
        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "content": "수정된 내용",
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));

        // 4. 게시글 삭제
        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 5. 삭제 후 조회 실패
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 작성 실패")
    void createPostWithoutAuthFail() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "비로그인 게시글",
                                  "content": "내용",
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 사용자는 게시글 수정 실패")
    void updateOtherUserPostFail() throws Exception {
        String ownerToken = loginAndExtractAccessToken(
                "post-user@test.com",
                "password123!"
        );

        String otherToken = loginAndExtractAccessToken(
                "other-user@test.com",
                "password123!"
        );

        // 게시글 생성
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "원본 제목",
                                  "content": "원본 내용",
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").exists())
                .andReturn();

        Long postId = extractLong(createResult, "postId");

        // 다른 사용자가 수정 시도
        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "해킹 수정",
                                  "content": "수정 불가",
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @SuppressWarnings("unchecked")
    private String loginAndExtractAccessToken(
            String email,
            String password
    ) throws Exception {

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

        Map<String, Object> data =
                (Map<String, Object>) responseBody.get("data");

        return String.valueOf(data.get("accessToken"));
    }

    @SuppressWarnings("unchecked")
    private Long extractLong(
            MvcResult result,
            String fieldName
    ) throws Exception {

        Map<String, Object> responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );

        Map<String, Object> data =
                (Map<String, Object>) responseBody.get("data");

        return toLong(data.get(fieldName));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}