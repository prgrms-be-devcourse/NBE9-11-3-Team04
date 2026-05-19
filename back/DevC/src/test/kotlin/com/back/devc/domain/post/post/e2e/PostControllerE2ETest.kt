package com.back.devc.domain.post.post.e2e

import com.fasterxml.jackson.databind.ObjectMapper
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
@DisplayName("게시글 E2E 테스트")
internal class PostControllerE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val objectMapper = ObjectMapper()

    private var userId = 0L
    private var otherUserId = 0L
    private var categoryId = 0L

    @BeforeEach
    fun setUp() {
        userId = 1L
        otherUserId = 2L
        categoryId = 1L

        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM USERS")
        jdbcTemplate.update("DELETE FROM CATEGORY")

        // 게시글 작성자
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
            "post-user@test.com",
            "게시글작성자",
            passwordEncoder.encode("password123!"),
            "LOCAL",
            "post-user@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        // 다른 사용자
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
            otherUserId,
            "other-user@test.com",
            "다른사용자",
            passwordEncoder.encode("password123!"),
            "LOCAL",
            "other-user@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        // 카테고리
        jdbcTemplate.update(
            """
                INSERT INTO CATEGORY (
                    CATEGORY_ID,
                    NAME
                ) VALUES (?, ?)
            """.trimIndent(),
            categoryId,
            "자유게시판",
        )
    }

    @Test
    @DisplayName("사용자 게시글 작성 → 조회 → 수정 → 삭제 전체 시나리오")
    fun createReadUpdateDeletePost() {
        val accessToken = loginAndExtractAccessToken(
            email = "post-user@test.com",
            password = "password123!",
        )

        // 1. 게시글 작성
        val createResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "E2E 테스트 제목",
                      "content": "E2E 테스트 내용",
                      "categoryId": 1
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_201_CREATE_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").exists())
            .andReturn()

        val postId = extractLong(createResult, "postId")

        // 2. 게시글 상세 조회
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/{postId}", postId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").value(postId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("E2E 테스트 제목"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("E2E 테스트 내용"))

        // 3. 게시글 수정
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/posts/{postId}", postId)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "수정된 제목",
                      "content": "수정된 내용",
                      "categoryId": 1
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("수정된 제목"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("수정된 내용"))

        // 4. 게시글 삭제
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/posts/{postId}", postId)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // 5. 삭제 후 조회 실패
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/{postId}", postId))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 작성 실패")
    fun createPostWithoutAuthFail() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "비로그인 게시글",
                      "content": "내용",
                      "categoryId": 1
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @DisplayName("다른 사용자는 게시글 수정 실패")
    fun updateOtherUserPostFail() {
        val ownerToken = loginAndExtractAccessToken(
            email = "post-user@test.com",
            password = "password123!",
        )

        val otherToken = loginAndExtractAccessToken(
            email = "other-user@test.com",
            password = "password123!",
        )

        // 게시글 생성
        val createResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "원본 제목",
                      "content": "원본 내용",
                      "categoryId": 1
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").exists())
            .andReturn()

        val postId = extractLong(createResult, "postId")

        // 다른 사용자가 수정 시도
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/posts/{postId}", postId)
                .header("Authorization", "Bearer $otherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "해킹 수정",
                      "content": "수정 불가",
                      "categoryId": 1
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
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