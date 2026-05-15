package com.back.devc.domain.post.comment;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("댓글/알림 E2E 테스트")
class CommentNotificationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long postId;
    private Long postOwnerId;
    private Long commentWriterId;

    @BeforeEach
    void setUp() {
        postOwnerId = 1L;
        commentWriterId = 2L;
        postId = 1L;

        // E2E 테스트가 기존 테스트 데이터에 의존하지 않도록 실제 H2 테이블을 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS");
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS");
        jdbcTemplate.update("DELETE FROM COMMENTS");
        jdbcTemplate.update("DELETE FROM POST_LIKES");
        jdbcTemplate.update("DELETE FROM BOOKMARKS");
        jdbcTemplate.update("DELETE FROM REPORTS");
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS");
        jdbcTemplate.update("DELETE FROM POST");
        jdbcTemplate.update("DELETE FROM USERS");

        // 게시글 작성자 사용자 A를 생성한다.
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
                postOwnerId,
                "post-owner@test.com",
                "게시글작성자",
                passwordEncoder.encode("password123!"),
                "LOCAL",
                "post-owner@test.com",
                "USER",
                "ACTIVE",
                null
        );

        // 댓글 작성자 사용자 B를 생성한다.
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
                commentWriterId,
                "comment-writer@test.com",
                "댓글작성자",
                passwordEncoder.encode("password123!"),
                "LOCAL",
                "comment-writer@test.com",
                "USER",
                "ACTIVE",
                null
        );

        // 사용자 A가 작성한 게시글을 생성한다.
        jdbcTemplate.update("""
                INSERT INTO POST (
                    POST_ID,
                    COMMENT_COUNT,
                    CONTENT,
                    CREATED_AT,
                    DELETED_AT,
                    IS_DELETED,
                    LIKE_COUNT,
                    TITLE,
                    UPDATED_AT,
                    VIEW_COUNT,
                    CATEGORY_ID,
                    USER_ID
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)
                """,
                postId,
                0,
                "E2E 테스트 게시글 내용",
                null,
                false,
                0,
                "E2E 테스트 게시글",
                0,
                1L,
                postOwnerId
        );
    }

    @Test
    @DisplayName("사용자 B가 사용자 A의 게시글에 댓글을 작성하면 사용자 A가 댓글 알림을 조회하고 읽음 처리할 수 있다")
    void createComment_thenGetNotification_andReadNotification() throws Exception {
        // given
        String commentWriterAccessToken = loginAndExtractAccessToken("comment-writer@test.com", "password123!");
        String postOwnerAccessToken = loginAndExtractAccessToken("post-owner@test.com", "password123!");

        // 1. 사용자 B가 사용자 A의 게시글 상세 페이지에서 댓글을 작성한다.
        MvcResult commentResult = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + commentWriterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "E2E 테스트 댓글입니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentId").exists())
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.userId").value(commentWriterId))
                .andExpect(jsonPath("$.data.content").value("E2E 테스트 댓글입니다."))
                .andReturn();

        Long commentId = extractLong(commentResult, "commentId");

        // 2. 사용자 A가 HTTP API로 알림 목록에서 댓글 알림을 조회한다.
        MvcResult notificationListResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + postOwnerAccessToken)
                        .param("page", "0")
                        .param("size", "20")
                        .param("tab", "comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications").isArray())
                .andReturn();

        Map<String, Object> createdNotification = findCommentNotification(notificationListResult, commentId);

        assertThat(createdNotification.get("type")).isEqualTo("COMMENT");
        assertThat(toLong(createdNotification.get("actorUserId"))).isEqualTo(commentWriterId);
        assertThat(toLong(createdNotification.get("postId"))).isEqualTo(postId);
        assertThat(toLong(createdNotification.get("commentId"))).isEqualTo(commentId);
        assertThat(createdNotification.get("isRead")).isEqualTo(false);

        Long notificationId = toLong(createdNotification.get("notificationId"));

        // 3. 사용자 A가 HTTP API로 해당 알림을 읽음 처리한다.
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + postOwnerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(notificationId))
                .andExpect(jsonPath("$.data.isRead").value(true));
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
    private Map<String, Object> findCommentNotification(MvcResult result, Long commentId) throws Exception {
        Map<String, Object> responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) data.get("notifications");

        return notifications.stream()
                .filter(notification -> "COMMENT".equals(notification.get("type")))
                .filter(notification -> commentId.equals(toLong(notification.get("commentId"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("댓글 작성 후 생성된 댓글 알림을 찾을 수 없습니다."));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}