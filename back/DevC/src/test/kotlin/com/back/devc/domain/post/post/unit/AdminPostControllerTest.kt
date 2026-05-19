package com.back.devc.domain.post.post.unit

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class AdminPostControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    private lateinit var member: Member
    private lateinit var adminMember: Member
    private lateinit var category: Category

    private val userToken: String
        get() = jwtProvider.createAccessToken(member)

    private val adminToken: String
        get() = jwtProvider.createAccessToken(adminMember)

    @BeforeEach
    fun setUp() {
        postRepository.deleteAll()
        categoryRepository.deleteAll()
        memberRepository.deleteAll()

        member = memberRepository.save(
            Member.createLocalMember("test@test.com", "password123!", "testUser"),
        )

        adminMember = memberRepository.save(
            Member.createLocalAdminMember("admin@test.com", "password123!", "adminUser"),
        )

        category = categoryRepository.save(Category("테스트 자유"))
    }

    @Test
    @DisplayName("관리자가 아니면 관리자 API 접근 불가")
    fun accessAdminPostsFailWhenNotAdmin() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/posts")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DisplayName("게시글 전체 조회 - 관리자용 (삭제 포함, 개수 유지)")
    fun getAdminPostsSuccessIncludingDeletedPost() {
        postRepository.save(Post.create(member, category, "글1", "내용1"))
        val post2 = postRepository.save(Post.create(member, category, "글2", "내용2"))
        postRepository.save(Post.create(member, category, "글3", "내용3"))

        post2.delete()
        postRepository.saveAndFlush(post2)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/posts")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].isDeleted").value(true))
    }

    @Test
    @DisplayName("게시글 상세 조회 - 관리자용 (삭제된 게시글도 조회 가능)")
    fun getAdminPostDetailSuccessWhenPostDeleted() {
        val deletedPost = postRepository.save(Post.create(member, category, "삭제된 글", "내용"))

        deletedPost.delete()
        postRepository.saveAndFlush(deletedPost)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/posts/{postId}", deletedPost.postId)
                .header("Authorization", "Bearer $adminToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("삭제된 글"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.isDeleted").value(true))
    }
}