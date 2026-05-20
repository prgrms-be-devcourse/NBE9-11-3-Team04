package com.back.devc.domain.interaction.e2e

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import kotlin.collections.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("좋아요/북마크/마이페이지 E2E 테스트")
class UserActivityE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbcTemplate: JdbcTemplate,
    private val passwordEncoder: PasswordEncoder,
) {

    private val objectMapper = ObjectMapper()

    private var userId: Long = 0L
    private var postOwnerId: Long = 0L
    private var postId: Long = 0L
    private var categoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = 1L
        postOwnerId = 2L
        postId = 1L
        categoryId = 1L

        jdbcTemplate.update("DELETE FROM NOTIFICATIONS")
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS")
        jdbcTemplate.update("DELETE FROM COMMENTS")
        jdbcTemplate.update("DELETE FROM POST_LIKES")
        jdbcTemplate.update("DELETE FROM BOOKMARKS")
        jdbcTemplate.update("DELETE FROM REPORTS")
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS")
        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM CATEGORY")
        jdbcTemplate.update("DELETE FROM USERS")

        jdbcTemplate.update(
            """
                INSERT INTO CATEGORY (
                    CATEGORY_ID,
                    NAME
                ) VALUES (?, ?)
            """.trimIndent(),
            categoryId,
            "테스트 카테고리",
        )

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
            "activity-user@test.com",
            "활동사용자",
            passwordEncoder.encode("password123!"),
            "LOCAL",
            "activity-user@test.com",
            "USER",
            "ACTIVE",
            null,
        )

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
            categoryId,
            postOwnerId,
        )
    }

    @Test
    @DisplayName("사용자가 게시글에 좋아요와 북마크를 누르면 마이페이지에서 각각 조회할 수 있다")
    fun likeAndBookmarkPostThenGetFromMyPage() {
        // given
        val accessToken = loginAndExtractAccessToken(
            email = "activity-user@test.com",
            password = "password123!",
        )

        // 1. 게시글 좋아요
        mockMvc.perform(
            post("/api/posts/{postId}/likes", postId)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("POST_LIKE_CREATED"))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.liked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(1))

        // 2. 게시글 북마크
        mockMvc.perform(
            post("/api/posts/{postId}/bookmarks", postId)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("BOOKMARK_201_CREATE"))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.bookmarked").value(true))

        // 3. DB 상태 확인
        val likeCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM POST_LIKES
                WHERE USER_ID = ?
                  AND POST_ID = ?
            """.trimIndent(),
            Int::class.java,
            userId,
            postId,
        )

        val bookmarkCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM BOOKMARKS
                WHERE USER_ID = ?
                  AND POST_ID = ?
            """.trimIndent(),
            Int::class.java,
            userId,
            postId,
        )

        assertThat(likeCount).isEqualTo(1)
        assertThat(bookmarkCount).isEqualTo(1)

        // 4. 마이페이지 - 내가 좋아요한 게시글 조회
        val likedPostsResult = mockMvc.perform(
            get("/api/mypage/likes")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MYPAGE_200_LIKES_FETCH"))
            .andReturn()

        val likedPost = findPostFromPagedResponse(likedPostsResult, postId)

        assertThat(toLong(likedPost["postId"])).isEqualTo(postId)

        // 5. 마이페이지 - 내가 북마크한 게시글 조회
        val bookmarkedPostsResult = mockMvc.perform(
            get("/api/mypage/bookmarks")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MYPAGE_200_BOOKMARKS_FETCH"))
            .andReturn()

        val bookmarkedPost = findPostFromPagedResponse(bookmarkedPostsResult, postId)

        assertThat(toLong(bookmarkedPost["postId"])).isEqualTo(postId)
    }

    @Test
    @DisplayName("사용자가 좋아요와 북마크를 취소하면 마이페이지 목록에서 조회되지 않는다")
    fun unlikeAndUnbookmarkPostThenDisappearFromMyPage() {
        // given
        val accessToken = loginAndExtractAccessToken(
            email = "activity-user@test.com",
            password = "password123!",
        )

        jdbcTemplate.update(
            """
                INSERT INTO POST_LIKES (
                    USER_ID,
                    POST_ID,
                    CREATED_AT
                ) VALUES (?, ?, CURRENT_TIMESTAMP)
            """.trimIndent(),
            userId,
            postId,
        )

        jdbcTemplate.update(
            """
                INSERT INTO BOOKMARKS (
                    USER_ID,
                    POST_ID,
                    CREATED_AT
                ) VALUES (?, ?, CURRENT_TIMESTAMP)
            """.trimIndent(),
            userId,
            postId,
        )

        // 1. 좋아요 취소
        mockMvc.perform(
            delete("/api/posts/{postId}/likes", postId)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("POST_LIKE_CANCELED"))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.liked").value(false))

        // 2. 북마크 취소
        mockMvc.perform(
            delete("/api/posts/{postId}/bookmarks", postId)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("BOOKMARK_200_DELETE"))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.bookmarked").value(false))

        // 3. DB 상태 확인
        val likeCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM POST_LIKES
                WHERE USER_ID = ?
                  AND POST_ID = ?
            """.trimIndent(),
            Int::class.java,
            userId,
            postId,
        )

        val bookmarkCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM BOOKMARKS
                WHERE USER_ID = ?
                  AND POST_ID = ?
            """.trimIndent(),
            Int::class.java,
            userId,
            postId,
        )

        assertThat(likeCount).isZero()
        assertThat(bookmarkCount).isZero()

        // 4. 마이페이지 좋아요 목록에서 사라졌는지 확인
        val likedPostsResult = mockMvc.perform(
            get("/api/mypage/likes")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MYPAGE_200_LIKES_FETCH"))
            .andReturn()

        assertPostNotExists(likedPostsResult, postId)

        // 5. 마이페이지 북마크 목록에서 사라졌는지 확인
        val bookmarkedPostsResult = mockMvc.perform(
            get("/api/mypage/bookmarks")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MYPAGE_200_BOOKMARKS_FETCH"))
            .andReturn()

        assertPostNotExists(bookmarkedPostsResult, postId)
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 좋아요, 북마크, 마이페이지 API를 사용할 수 없다")
    fun unauthenticatedUserFail() {
        mockMvc.perform(post("/api/posts/{postId}/likes", postId))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/posts/{postId}/bookmarks", postId))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/mypage/likes")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/mypage/bookmarks")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isUnauthorized)
    }

    private fun loginAndExtractAccessToken(
        email: String,
        password: String,
    ): String {
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
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andReturn()

        val responseBody: Map<String, Any?> = objectMapper.readValue(
            loginResult.response.contentAsString,
            object : TypeReference<Map<String, Any?>>() {},
        )

        val data = responseBody["data"] as? Map<*, *>
            ?: throw AssertionError("로그인 응답에서 data 필드를 찾을 수 없습니다. response=$responseBody")

        return data["accessToken"].toString()
    }

    private fun findPostFromPagedResponse(
        result: MvcResult,
        postId: Long,
    ): Map<String, Any?> {
        val posts = extractContentList(result)

        return posts
            .firstOrNull { post ->
                Objects.equals(postId, toLong(post["postId"]))
            }
            ?: throw AssertionError("마이페이지 목록에서 postId=$postId 게시글을 찾을 수 없습니다.")
    }

    private fun assertPostNotExists(
        result: MvcResult,
        postId: Long,
    ) {
        val posts = extractContentList(result)

        val exists = posts.any { post ->
            Objects.equals(postId, toLong(post["postId"]))
        }

        assertThat(exists).isFalse()
    }

    private fun extractContentList(result: MvcResult): List<Map<String, Any?>> {
        val responseBody: Map<String, Any?> = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<Map<String, Any?>>() {},
        )

        val data = responseBody["data"] as? Map<*, *>
            ?: throw AssertionError("PageResponse 응답에서 data 필드를 찾을 수 없습니다. response=$responseBody")

        if (!data.containsKey("content")) {
            throw AssertionError("PageResponse 응답에서 content 필드를 찾을 수 없습니다. data=$data")
        }

        @Suppress("UNCHECKED_CAST")
        return data["content"] as List<Map<String, Any?>>
    }

    private fun toLong(value: Any?): Long? {
        if (value == null) {
            return null
        }

        return when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLong()
        }
    }
}