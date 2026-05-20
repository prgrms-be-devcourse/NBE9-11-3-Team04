package com.back.devc.domain.interaction.report.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Report E2E")
internal class ReportE2ETest {
    @Autowired
    private val mockMvc: MockMvc? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Autowired
    private val passwordEncoder: PasswordEncoder? = null

    private val objectMapper = ObjectMapper()

    private var reporterId: Long? = null
    private var secondReporterId: Long? = null
    private var postOwnerId: Long? = null
    private var adminId: Long? = null
    private var categoryId: Long? = null
    private var postId: Long? = null
    private var commentId: Long? = null


    @BeforeEach
    fun setUp() {
        reporterId = 101L
        secondReporterId = 102L
        postOwnerId = 201L
        adminId = 901L
        categoryId = 301L
        postId = 401L
        commentId = 501L

        cleanDatabase()

        insertMember(reporterId, "reporter-e2e@test.com", "reporter-e2e", "USER", "ACTIVE")
        insertMember(secondReporterId, "second-reporter-e2e@test.com", "second-reporter-e2e", "USER", "ACTIVE")
        insertMember(postOwnerId, "post-owner-e2e@test.com", "post-owner-e2e", "USER", "ACTIVE")
        insertMember(adminId, "admin-e2e@test.com", "admin-e2e", "ADMIN", "ACTIVE")

        jdbcTemplate!!.update(
            """
                        INSERT INTO CATEGORY (
                            CATEGORY_ID,
                            NAME
                        ) VALUES (?, ?)
                        
                        """.trimIndent(),
            categoryId,
            "report-e2e-category"
        )

        insertPost(postId, postOwnerId, "Report E2E Post", "Report E2E post content")
        insertComment(commentId, postId, postOwnerId, "Report E2E comment content")
    }

    @AfterEach
    fun tearDown() {
        cleanDatabase()
        seedDefaultCategory()
    }

    @Test
    @DisplayName("users report a post, admin approves the report group, and target owner is notified and sanctioned")
    @Throws(
        Exception::class
    )
    fun reportPostGroupApprovalFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")
        val secondReporterToken = loginAndExtractAccessToken("second-reporter-e2e@test.com", "password123!")
        val adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!")

        reportPost(reporterToken, postId, "SPAM")
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_201"))

        reportPost(reporterToken, postId, "SPAM")
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_409"))

        reportPost(secondReporterToken, postId, "ABUSE")
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_201"))

        Assertions.assertThat(countReports("POST", postId)).isEqualTo(2)

        val now = LocalDateTime.now()
        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/api/admin/reports/groups")
                .header("Authorization", "Bearer " + adminToken)
                .param("status", "PENDING")
                .param("from", now.minusDays(1).toString())
                .param("to", now.plusDays(1).toString())
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath<MutableCollection<*>?>(
                    "$.data.content",
                    Matchers.hasSize<Any?>(1)
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetType").value("POST"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetId").value(postId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetNickname").value("post-owner-e2e"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetTitle").value("Report E2E Post"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].reportCount").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath<Iterable<out String?>?>(
                    "$.data.content[0].reasonTypes",
                    Matchers.containsInAnyOrder<String?>("SPAM", "ABUSE")
                )
            )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/admin/reports/groups/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "reportId": %d,
                                  "targetType": "POST",
                                  "adminNote": "E2E approval",
                                  "sanctionType": "WARNED",
                                  "suspensionDays": null
                                }
                                
                                """.trimIndent().formatted(postId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_200"))

        Assertions.assertThat<String?>(findReportStatuses("POST", postId))
            .containsExactlyInAnyOrder("RESOLVED", "RESOLVED")
        Assertions.assertThat(findPostDeleted(postId)).isTrue()
        Assertions.assertThat(findMemberStatus(postOwnerId)).isEqualTo("WARNED")

        val notificationCount: Int? = jdbcTemplate!!.queryForObject<Int?>(
            """
                        SELECT COUNT(*)
                        FROM NOTIFICATIONS
                        WHERE USER_ID = ?
                          AND ACTOR_USER_ID = ?
                          AND POST_ID = ?
                          AND TYPE = 'REPORT'
                        
                        """.trimIndent(),
            Int::class.java,
            postOwnerId,
            adminId,
            postId
        )
        Assertions.assertThat(notificationCount).isEqualTo(1)
    }

    @Test
    @DisplayName("user reports a comment and admin rejects the report group without deleting the comment")
    @Throws(
        Exception::class
    )
    fun reportCommentGroupRejectFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")
        val adminToken = loginAndExtractAccessToken("admin-e2e@test.com", "password123!")

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/comment")
                .header("Authorization", "Bearer " + reporterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "targetId": %d,
                                  "reasonType": "ABUSE",
                                  "reasonDetail": "Comment is abusive"
                                }
                                
                                """.trimIndent().formatted(commentId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_201"))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/admin/reports/groups/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "reportId": %d,
                                  "targetType": "COMMENT",
                                  "adminNote": "E2E rejection",
                                  "sanctionType": null,
                                  "suspensionDays": null
                                }
                                
                                """.trimIndent().formatted(commentId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_200"))

        Assertions.assertThat<String?>(findReportStatuses("COMMENT", commentId))
            .containsExactly("REJECTED")
        Assertions.assertThat(findCommentDeleted(commentId)).isFalse()
        Assertions.assertThat(findMemberStatus(postOwnerId)).isEqualTo("ACTIVE")
    }

    @Test
    @DisplayName("unauthenticated users cannot report and normal users cannot access admin report APIs")
    @Throws(
        Exception::class
    )
    fun reportSecurityFlow() {
        val reporterToken = loginAndExtractAccessToken("reporter-e2e@test.com", "password123!")

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "targetId": %d,
                                  "reasonType": "SPAM",
                                  "reasonDetail": "No token"
                                }
                                
                                """.trimIndent().formatted(postId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/reports/groups")
                .header("Authorization", "Bearer " + reporterToken)
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    private fun insertMember(userId: Long?, email: String?, nickname: String?, role: String?, status: String?) {
        jdbcTemplate!!.update(
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
            passwordEncoder!!.encode("password123!"),
            "LOCAL",
            email,
            role,
            status,
            null
        )
    }

    private fun cleanDatabase() {
        jdbcTemplate!!.update("DELETE FROM NOTIFICATIONS")
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS")
        jdbcTemplate.update("DELETE FROM COMMENTS")
        jdbcTemplate.update("DELETE FROM POST_LIKES")
        jdbcTemplate.update("DELETE FROM BOOKMARKS")
        jdbcTemplate.update("DELETE FROM REPORTS")
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS")
        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM CATEGORY")
        jdbcTemplate.update("DELETE FROM USERS")
    }

    private fun seedDefaultCategory() {
        jdbcTemplate!!.update(
            """
                        INSERT INTO CATEGORY (
                            CATEGORY_ID,
                            NAME
                        ) VALUES (?, ?)
                        
                        """.trimIndent(),
            1L,
            "default-category"
        )
    }

    private fun insertPost(postId: Long?, userId: Long?, title: String?, content: String?) {
        jdbcTemplate!!.update(
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
            categoryId,
            userId
        )
    }

    private fun insertComment(commentId: Long?, postId: Long?, userId: Long?, content: String?) {
        jdbcTemplate!!.update(
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

    @Throws(Exception::class)
    private fun reportPost(accessToken: String?, targetId: Long?, reasonType: String?): ResultActions {
        return mockMvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/post")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "targetId": %d,
                          "reasonType": "%s",
                          "reasonDetail": "E2E report detail"
                        }
                        
                        """.trimIndent().formatted(targetId, reasonType)
                )
        )
    }

    @Throws(Exception::class)
    private fun loginAndExtractAccessToken(email: String?, password: String?): String? {
        val loginResult = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                
                                """.trimIndent().formatted(email, password)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn()

        val responseBody: MutableMap<String?, Any?> = objectMapper.readValue<MutableMap<*, *>>(
            loginResult.getResponse().getContentAsString(),
            MutableMap::class.java
        )

        val data = responseBody.get("data") as MutableMap<String?, Any?>
        return data.get("accessToken").toString()
    }

    private fun countReports(targetType: String?, targetId: Long?): Int? {
        return jdbcTemplate.queryForObject<Int?>(
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
    }

    private fun findReportStatuses(targetType: String?, targetId: Long?): MutableList<String?> {
        return jdbcTemplate!!.queryForList<String?>(
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
        )
    }

    private fun findPostDeleted(postId: Long?): Boolean? {
        return jdbcTemplate.queryForObject<Boolean?>(
            """
                        SELECT IS_DELETED
                        FROM POST
                        WHERE POST_ID = ?
                        
                        """.trimIndent(),
            Boolean::class.java,
            postId
        )
    }

    private fun findCommentDeleted(commentId: Long?): Boolean? {
        return jdbcTemplate.queryForObject<Boolean?>(
            """
                        SELECT IS_DELETED
                        FROM COMMENTS
                        WHERE COMMENT_ID = ?
                        
                        """.trimIndent(),
            Boolean::class.java,
            commentId
        )
    }

    private fun findMemberStatus(userId: Long?): String? {
        return jdbcTemplate.queryForObject<String?>(
            """
                        SELECT STATUS
                        FROM USERS
                        WHERE USER_ID = ?
                        
                        """.trimIndent(),
            String::class.java,
            userId
        )
    }
}
