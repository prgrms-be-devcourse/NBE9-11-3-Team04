package com.back.devc.domain.post.comment.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.collections.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("댓글/알림 E2E 테스트")
internal class CommentNotificationE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val objectMapper = ObjectMapper()

    private var postId = 0L
    private var postOwnerId = 0L
    private var commentWriterId = 0L

    @BeforeEach
    fun setUp() {
        postOwnerId = 1L
        commentWriterId = 2L
        postId = 1L

        // E2E 테스트가 기존 테스트 데이터에 의존하지 않도록 실제 H2 테이블을 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS")
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS")
        jdbcTemplate.update("DELETE FROM COMMENTS")
        jdbcTemplate.update("DELETE FROM POST_LIKES")
        jdbcTemplate.update("DELETE FROM BOOKMARKS")
        jdbcTemplate.update("DELETE FROM REPORTS")
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS")
        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM USERS")

        // 게시글 작성자 사용자 A를 생성한다.
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
            postOwnerId,
            "post-owner@test.com",
            "게시글작성자",
            passwordEncoder.encode("password123!"),
            "LOCAL",
            "post-owner@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        // 댓글 작성자 사용자 B를 생성한다.
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
            commentWriterId,
            "comment-writer@test.com",
            "댓글작성자",
            passwordEncoder.encode("password123!"),
            "LOCAL",
            "comment-writer@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        // 사용자 A가 작성한 게시글을 생성한다.
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
            "E2E 테스트 게시글 내용",
            null,
            false,
            0,
            "E2E 테스트 게시글",
            0,
            1L,
            postOwnerId,
        )
    }

    @Test
    @DisplayName("사용자 B가 사용자 A의 게시글에 댓글을 작성하면 사용자 A가 댓글 알림을 조회하고 읽음 처리할 수 있다")
    fun createCommentThenGetNotificationAndReadNotification() {
        val commentWriterAccessToken = loginAndExtractAccessToken("comment-writer@test.com", "password123!")
        val postOwnerAccessToken = loginAndExtractAccessToken("post-owner@test.com", "password123!")

        // 1. 사용자 B가 사용자 A의 게시글 상세 페이지에서 댓글을 작성한다.
        val commentResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts/{postId}/comments", postId)
                .header("Authorization", "Bearer $commentWriterAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "E2E 테스트 댓글입니다."
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.commentId").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").value(postId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(commentWriterId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("E2E 테스트 댓글입니다."))
            .andReturn()

        val commentId = extractLong(commentResult, "commentId")

        // 2. 사용자 A가 HTTP API로 알림 목록에서 댓글 알림을 조회한다.
        val notificationListResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/notifications")
                .header("Authorization", "Bearer $postOwnerAccessToken")
                .param("page", "0")
                .param("size", "20")
                .param("tab", "comments"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.notifications").isArray)
            .andReturn()

        val createdNotification = findCommentNotification(notificationListResult, commentId)

        Assertions.assertThat(createdNotification["type"]).isEqualTo("COMMENT")
        Assertions.assertThat(toLong(createdNotification["actorUserId"])).isEqualTo(commentWriterId)
        Assertions.assertThat(toLong(createdNotification["postId"])).isEqualTo(postId)
        Assertions.assertThat(toLong(createdNotification["commentId"])).isEqualTo(commentId)
        Assertions.assertThat(createdNotification["isRead"]).isEqualTo(false)

        val notificationId = toLong(createdNotification["notificationId"])

        // 3. 사용자 A가 HTTP API로 해당 알림을 읽음 처리한다.
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/notifications/{notificationId}/read", notificationId)
                .header("Authorization", "Bearer $postOwnerAccessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.notificationId").value(notificationId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isRead").value(true))
    }

    private fun loginAndExtractAccessToken(
        email: String,
        password: String,
    ): String {
        val loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty)
            .andReturn()

        val responseBody = readResponseBody(loginResult)
        val data = responseBody["data"] as Map<*, *>

        return data["accessToken"].toString()
    }

    private fun extractLong(
        result: MvcResult,
        fieldName: String,
    ): Long {
        val responseBody = readResponseBody(result)
        val data = responseBody["data"] as Map<*, *>

        return toLong(data[fieldName])
    }

    private fun findCommentNotification(
        result: MvcResult,
        commentId: Long,
    ): Map<*, *> {
        val responseBody = readResponseBody(result)
        val data = responseBody["data"] as Map<*, *>
        val notifications = data["notifications"] as List<Map<*, *>>

        return notifications.firstOrNull { notification ->
            notification["type"] == "COMMENT" && commentId == toLong(notification["commentId"])
        } ?: throw AssertionError("댓글 작성 후 생성된 댓글 알림을 찾을 수 없습니다.")
    }

    private fun readResponseBody(result: MvcResult): Map<*, *> {
        return objectMapper.readValue(
            result.response.contentAsString,
            Map::class.java,
        )
    }

    private fun toLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLong()
        }
    }
}