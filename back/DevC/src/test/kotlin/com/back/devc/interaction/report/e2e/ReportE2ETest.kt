package com.back.devc.interaction.report.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Report E2E")
internal class ReportE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbcTemplate: JdbcTemplate,
    private val passwordEncoder: PasswordEncoder
) {
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        cleanDatabase()

        insertMember(REPORTER_ID, "reporter-e2e@test.com", "reporter-e2e", "USER", "ACTIVE")
        insertMember(SECOND_REPORTER_ID, "second-reporter-e2e@test.com", "second-reporter-e2e", "USER", "ACTIVE")
        insertMember(POST_OWNER_ID, "post-owner-e2e@test.com", "post-owner-e2e", "USER", "ACTIVE")
        insertMember(ADMIN_ID, "admin-e2e@test.com", "admin-e2e", "ADMIN", "ACTIVE")
        insertCategory(CATEGORY_ID, "report-e2e-category")
        insertPost(POST_ID, POST_OWNER_ID, "Report E2E Post", "Report E2E post content")
        insertComment(COMMENT_ID, POST_ID, POST_OWNER_ID, "Report E2E comment content")
    }

    @AfterEach
    fun tearDown() {
        cleanDatabase()
        insertCategory(1L, "default-category")
    }

    @Test
    @DisplayName("users report a post, admin approves the report group, and target owner is notified and sanctioned")
    fun reportPostGroupApprovalFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")
        val secondReporterToken = loginAndExtractAccessToken("second-reporter-e2e@test.com", "password123!")
        val adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!")

        reportPost(reporterToken, POST_ID, "SPAM")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("REPORT_201"))

        reportPost(reporterToken, POST_ID, "SPAM")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REPORT_409"))

        reportPost(secondReporterToken, POST_ID, "ABUSE")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("REPORT_201"))

        assertThat(countReports("POST", POST_ID)).isEqualTo(2)

        val now = LocalDateTime.now()
        mockMvc.perform(
            get("/api/admin/reports/groups")
                .header("Authorization", "Bearer $adminToken")
                .param("status", "PENDING")
                .param("from", now.minusDays(1).toString())
                .param("to", now.plusDays(1).toString())
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("REPORT_200"))
            .andExpect(jsonPath("$.data.content", hasSize<Any>(1)))
            .andExpect(jsonPath("$.data.content[0].targetType").value("POST"))
            .andExpect(jsonPath("$.data.content[0].targetId").value(POST_ID))
            .andExpect(jsonPath("$.data.content[0].targetNickname").value("post-owner-e2e"))
            .andExpect(jsonPath("$.data.content[0].targetTitle").value("Report E2E Post"))
            .andExpect(jsonPath("$.data.content[0].reportCount").value(2))
            .andExpect(jsonPath("$.data.content[0].reasonTypes", containsInAnyOrder("SPAM", "ABUSE")))

        mockMvc.perform(
            post("/api/admin/reports/groups/approve")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reportId": $POST_ID,
                      "targetType": "POST",
                      "adminNote": "E2E approval",
                      "sanctionType": "WARNED",
                      "suspensionDays": null
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("REPORT_200"))

        assertThat(findReportStatuses("POST", POST_ID)).containsExactlyInAnyOrder("RESOLVED", "RESOLVED")
        assertThat(findPostDeleted(POST_ID)).isTrue()
        assertThat(findMemberStatus(POST_OWNER_ID)).isEqualTo("WARNED")
        assertThat(countPostReportNotifications()).isEqualTo(1)
    }

    @Test
    @DisplayName("user reports a comment and admin rejects the report group without deleting the comment")
    fun reportCommentGroupRejectFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")
        val adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!")

        mockMvc.perform(
            post("/api/report/comment")
                .header("Authorization", "Bearer $reporterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetId": $COMMENT_ID,
                      "reasonType": "ABUSE",
                      "reasonDetail": "Comment is abusive"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("REPORT_201"))

        mockMvc.perform(
            post("/api/admin/reports/groups/reject")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reportId": $COMMENT_ID,
                      "targetType": "COMMENT",
                      "adminNote": "E2E rejection",
                      "sanctionType": null,
                      "suspensionDays": null
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("REPORT_200"))

        assertThat(findReportStatuses("COMMENT", COMMENT_ID)).containsExactly("REJECTED")
        assertThat(findCommentDeleted(COMMENT_ID)).isFalse()
        assertThat(findMemberStatus(POST_OWNER_ID)).isEqualTo("ACTIVE")
    }

    @Test
    @DisplayName("unauthenticated users cannot report and normal users cannot access admin report APIs")
    fun reportSecurityFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")

        mockMvc.perform(
            post("/api/report/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetId": $POST_ID,
                      "reasonType": "SPAM",
                      "reasonDetail": "No token"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isUnauthorized())

        mockMvc.perform(
            get("/api/admin/reports/groups")
                .header("Authorization", "Bearer $reporterToken")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isForbidden())
    }

    private fun insertMember(
        userId: Long,
        email: String,
        nickname: String,
        role: String,
        status: String
    ) {
        jdbcTemplate.update(
            """
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
            """.trimIndent(),
            userId,
            email,
            nickname,
            passwordEncoder.encode("password123!"),
            "LOCAL",
            email,
            role,
            status,
            null
        )
    }

    private fun insertCategory(categoryId: Long, name: String) {
        jdbcTemplate.update(
            """
            INSERT INTO CATEGORY (
                CATEGORY_ID,
                NAME
            ) VALUES (?, ?)
            """.trimIndent(),
            categoryId,
            name
        )
    }

    private fun insertPost(postId: Long, userId: Long, title: String, content: String) {
        jdbcTemplate.update(
            """
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
            """.trimIndent(),
            postId,
            0,
            content,
            null,
            false,
            0,
            title,
            0,
            CATEGORY_ID,
            userId
        )
    }

    private fun insertComment(commentId: Long, postId: Long, userId: Long, content: String) {
        jdbcTemplate.update(
            """
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
            """.trimIndent(),
            commentId,
            postId,
            userId,
            null,
            content,
            false,
            null
        )
    }

    private fun cleanDatabase() {
        listOf(
            "DELETE FROM NOTIFICATIONS",
            "DELETE FROM REPORT_GROUP_ACTIONS",
            "DELETE FROM REPORTS",
            "DELETE FROM REPORT_GROUPS",
            "DELETE FROM COMMENT_ATTACHMENTS",
            "DELETE FROM COMMENTS",
            "DELETE FROM POST_LIKES",
            "DELETE FROM BOOKMARKS",
            "DELETE FROM SEARCH_LOGS",
            "DELETE FROM POST",
            "DELETE FROM CATEGORY",
            "DELETE FROM USERS"
        ).forEach(jdbcTemplate::update)
    }

    private fun reportPost(accessToken: String, targetId: Long, reasonType: String): ResultActions =
        mockMvc.perform(
            post("/api/report/post")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetId": $targetId,
                      "reasonType": "$reasonType",
                      "reasonDetail": "E2E report detail"
                    }
                    """.trimIndent()
                )
        )

    private fun loginAndExtractAccessToken(email: String, password: String): String {
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn()

        val responseBody = objectMapper.readTree(loginResult.response.contentAsString)
        return responseBody.path("data").path("accessToken").asText()
    }

    private fun countReports(targetType: String, targetId: Long): Int =
        queryRequired(
            """
            SELECT COUNT(*)
            FROM REPORTS
            WHERE TARGET_TYPE = ?
              AND TARGET_ID = ?
            """.trimIndent(),
            Int::class.java,
            targetType,
            targetId
        )

    private fun countPostReportNotifications(): Int =
        queryRequired(
            """
            SELECT COUNT(*)
            FROM NOTIFICATIONS
            WHERE USER_ID = ?
              AND ACTOR_USER_ID = ?
              AND POST_ID = ?
              AND TYPE = 'REPORT'
            """.trimIndent(),
            Int::class.java,
            POST_OWNER_ID,
            ADMIN_ID,
            POST_ID
        )

    private fun findReportStatuses(targetType: String, targetId: Long): List<String> =
        jdbcTemplate.queryForList(
            """
        SELECT STATUS
        FROM REPORTS
        WHERE TARGET_TYPE = ?
          AND TARGET_ID = ?
        ORDER BY REPORT_ID
        """.trimIndent(),
            String::class.java,
            targetType,
            targetId
        ).filterNotNull()

    private fun findPostDeleted(postId: Long): Boolean =
        queryRequired(
            """
            SELECT IS_DELETED
            FROM POST
            WHERE POST_ID = ?
            """.trimIndent(),
            Boolean::class.java,
            postId
        )

    private fun findCommentDeleted(commentId: Long): Boolean =
        queryRequired(
            """
            SELECT IS_DELETED
            FROM COMMENTS
            WHERE COMMENT_ID = ?
            """.trimIndent(),
            Boolean::class.java,
            commentId
        )

    private fun findMemberStatus(userId: Long): String =
        queryRequired(
            """
            SELECT STATUS
            FROM USERS
            WHERE USER_ID = ?
            """.trimIndent(),
            String::class.java,
            userId
        )

    private fun <T : Any> queryRequired(sql: String, requiredType: Class<T>, vararg args: Any?): T =
        jdbcTemplate.queryForObject(sql, requiredType, *args)
            ?: throw AssertionError("Expected query result for SQL: $sql")

    private companion object {
        const val REPORTER_ID = 101L
        const val SECOND_REPORTER_ID = 102L
        const val POST_OWNER_ID = 201L
        const val ADMIN_ID = 901L
        const val CATEGORY_ID = 301L
        const val POST_ID = 401L
        const val COMMENT_ID = 501L
    }
}
