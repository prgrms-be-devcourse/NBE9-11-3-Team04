package com.back.devc.domain.interaction.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Report E2E")
class ReportE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long reporterId;
    private Long secondReporterId;
    private Long postOwnerId;
    private Long adminId;
    private Long categoryId;
    private Long postId;
    private Long commentId;

    @BeforeEach
    void setUp() {
        reporterId = 101L;
        secondReporterId = 102L;
        postOwnerId = 201L;
        adminId = 901L;
        categoryId = 301L;
        postId = 401L;
        commentId = 501L;

        cleanDatabase();

        insertMember(reporterId, "reporter-e2e@test.com", "reporter-e2e", "USER", "ACTIVE");
        insertMember(secondReporterId, "second-reporter-e2e@test.com", "second-reporter-e2e", "USER", "ACTIVE");
        insertMember(postOwnerId, "post-owner-e2e@test.com", "post-owner-e2e", "USER", "ACTIVE");
        insertMember(adminId, "admin-e2e@test.com", "admin-e2e", "ADMIN", "ACTIVE");

        jdbcTemplate.update("""
                        INSERT INTO CATEGORY (
                            CATEGORY_ID,
                            NAME
                        ) VALUES (?, ?)
                        """,
                categoryId,
                "report-e2e-category"
        );

        insertPost(postId, postOwnerId, "Report E2E Post", "Report E2E post content");
        insertComment(commentId, postId, postOwnerId, "Report E2E comment content");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        seedDefaultCategory();
    }

    @Test
    @DisplayName("users report a post, admin approves the report group, and target owner is notified and sanctioned")
    void reportPostGroupApprovalFlow() throws Exception {
        String reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!");
        String secondReporterToken = loginAndExtractAccessToken("second-reporter-e2e@test.com", "password123!");
        String adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!");

        reportPost(reporterToken, postId, "SPAM")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REPORT_201"));

        reportPost(reporterToken, postId, "SPAM")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_409"));

        reportPost(secondReporterToken, postId, "ABUSE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REPORT_201"));

        assertThat(countReports("POST", postId)).isEqualTo(2);

        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(get("/api/admin/reports/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING")
                        .param("from", now.minusDays(1).toString())
                        .param("to", now.plusDays(1).toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_200"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(postId))
                .andExpect(jsonPath("$.data.content[0].targetNickname").value("post-owner-e2e"))
                .andExpect(jsonPath("$.data.content[0].targetTitle").value("Report E2E Post"))
                .andExpect(jsonPath("$.data.content[0].reportCount").value(2))
                .andExpect(jsonPath("$.data.content[0].reasonTypes", containsInAnyOrder("SPAM", "ABUSE")));

        mockMvc.perform(post("/api/admin/reports/groups/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": %d,
                                  "targetType": "POST",
                                  "adminNote": "E2E approval",
                                  "sanctionType": "WARNED",
                                  "suspensionDays": null
                                }
                                """.formatted(postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_200"));

        assertThat(findReportStatuses("POST", postId))
                .containsExactlyInAnyOrder("RESOLVED", "RESOLVED");
        assertThat(findPostDeleted(postId)).isTrue();
        assertThat(findMemberStatus(postOwnerId)).isEqualTo("WARNED");

        Integer notificationCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM NOTIFICATIONS
                        WHERE USER_ID = ?
                          AND ACTOR_USER_ID = ?
                          AND POST_ID = ?
                          AND TYPE = 'REPORT'
                        """,
                Integer.class,
                postOwnerId,
                adminId,
                postId
        );
        assertThat(notificationCount).isEqualTo(1);
    }

    @Test
    @DisplayName("user reports a comment and admin rejects the report group without deleting the comment")
    void reportCommentGroupRejectFlow() throws Exception {
        String reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!");
        String adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!");

        mockMvc.perform(post("/api/report/comment")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %d,
                                  "reasonType": "ABUSE",
                                  "reasonDetail": "Comment is abusive"
                                }
                                """.formatted(commentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REPORT_201"));

        mockMvc.perform(post("/api/admin/reports/groups/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": %d,
                                  "targetType": "COMMENT",
                                  "adminNote": "E2E rejection",
                                  "sanctionType": null,
                                  "suspensionDays": null
                                }
                                """.formatted(commentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_200"));

        assertThat(findReportStatuses("COMMENT", commentId))
                .containsExactly("REJECTED");
        assertThat(findCommentDeleted(commentId)).isFalse();
        assertThat(findMemberStatus(postOwnerId)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("unauthenticated users cannot report and normal users cannot access admin report APIs")
    void reportSecurityFlow() throws Exception {
        String reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!");

        mockMvc.perform(post("/api/report/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %d,
                                  "reasonType": "SPAM",
                                  "reasonDetail": "No token"
                                }
                                """.formatted(postId)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/reports/groups")
                        .header("Authorization", "Bearer " + reporterToken)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    private void insertMember(Long userId, String email, String nickname, String role, String status) {
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
                email,
                nickname,
                passwordEncoder.encode("password123!"),
                "LOCAL",
                email,
                role,
                status,
                null
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS");
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS");
        jdbcTemplate.update("DELETE FROM COMMENTS");
        jdbcTemplate.update("DELETE FROM POST_LIKES");
        jdbcTemplate.update("DELETE FROM BOOKMARKS");
        jdbcTemplate.update("DELETE FROM REPORTS");
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS");
        jdbcTemplate.update("DELETE FROM POST");
        jdbcTemplate.update("DELETE FROM CATEGORY");
        jdbcTemplate.update("DELETE FROM USERS");
    }

    private void seedDefaultCategory() {
        jdbcTemplate.update("""
                        INSERT INTO CATEGORY (
                            CATEGORY_ID,
                            NAME
                        ) VALUES (?, ?)
                        """,
                1L,
                "default-category"
        );
    }

    private void insertPost(Long postId, Long userId, String title, String content) {
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
                content,
                null,
                false,
                0,
                title,
                0,
                categoryId,
                userId
        );
    }

    private void insertComment(Long commentId, Long postId, Long userId, String content) {
        jdbcTemplate.update("""
                        INSERT INTO COMMENTS (
                            COMMENT_ID,
                            POST_ID,
                            USER_ID,
                            PARENT_COMMENT_ID,
                            CONTENT,
                            IS_DELETED,
                            DELETED_AT,
                            CREATED_AT,
                            UPDATED_AT
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                commentId,
                postId,
                userId,
                null,
                content,
                false,
                null
        );
    }

    private ResultActions reportPost(String accessToken, Long targetId, String reasonType) throws Exception {
        return mockMvc.perform(post("/api/report/post")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "targetId": %d,
                          "reasonType": "%s",
                          "reasonDetail": "E2E report detail"
                        }
                        """.formatted(targetId, reasonType)));
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

    private Integer countReports(String targetType, Long targetId) {
        return jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM REPORTS
                        WHERE TARGET_TYPE = ?
                          AND TARGET_ID = ?
                        """,
                Integer.class,
                targetType,
                targetId
        );
    }

    private List<String> findReportStatuses(String targetType, Long targetId) {
        return jdbcTemplate.queryForList("""
                        SELECT STATUS
                        FROM REPORTS
                        WHERE TARGET_TYPE = ?
                          AND TARGET_ID = ?
                        ORDER BY REPORT_ID
                        """,
                String.class,
                targetType,
                targetId
        );
    }

    private Boolean findPostDeleted(Long postId) {
        return jdbcTemplate.queryForObject("""
                        SELECT IS_DELETED
                        FROM POST
                        WHERE POST_ID = ?
                        """,
                Boolean.class,
                postId
        );
    }

    private Boolean findCommentDeleted(Long commentId) {
        return jdbcTemplate.queryForObject("""
                        SELECT IS_DELETED
                        FROM COMMENTS
                        WHERE COMMENT_ID = ?
                        """,
                Boolean.class,
                commentId
        );
    }

    private String findMemberStatus(Long userId) {
        return jdbcTemplate.queryForObject("""
                        SELECT STATUS
                        FROM USERS
                        WHERE USER_ID = ?
                        """,
                String.class,
                userId
        );
    }
}
