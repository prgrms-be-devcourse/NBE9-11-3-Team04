package com.back.devc.domain.post.post.unit

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.security.jwt.JwtProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class PostControllerUnitTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    private lateinit var member: Member
    private lateinit var category: Category

    private val accessToken: String
        get() = jwtProvider.createAccessToken(member)

    @BeforeEach
    fun setUp() {
        val unique = System.nanoTime().toString()

        member = memberRepository.save(
            Member.createLocalMember(
                email = "post-controller-$unique@test.com",
                passwordHash = "password123!",
                nickname = "postControllerUser$unique",
            ),
        )

        category = categoryRepository.save(Category("테스트 자유 $unique"))
    }

    @Test
    @DisplayName("게시글 생성")
    fun createPostSuccess() {
        val request = PostCreateRequest(
            title = "테스트글",
            content = "테스트내용입니다.",
            categoryId = requireNotNull(category.categoryId),
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("게시글 작성 성공"))
    }

    @Test
    @DisplayName("게시글 수정")
    fun updatePostSuccess() {
        val post = postRepository.save(
            Post.create(member, category, "수정 전 제목", "수정 전 내용"),
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/posts/{postId}", post.postId)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "수정 후 제목",
                      "content": "수정 후 내용",
                      "categoryId": ${category.categoryId}
                    }
                    """.trimIndent(),
                ),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("수정 후 제목"))
    }

    @Test
    @DisplayName("게시글 삭제")
    fun deletePostSuccess() {
        val post = postRepository.save(
            Post.create(member, category, "title", "content"),
        )

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/posts/{postId}", post.postId)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("게시글 삭제 성공"))

        val deleted = postRepository.findById(requireNotNull(post.postId)).orElseThrow()

        Assertions.assertThat(deleted.isDeleted).isTrue()
    }
}