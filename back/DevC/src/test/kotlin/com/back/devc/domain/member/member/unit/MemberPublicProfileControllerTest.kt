package com.back.devc.domain.member.member.unit

import com.back.devc.domain.member.member.controller.MemberController
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
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
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class MemberPublicProfileControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    @DisplayName("공개 프로필 조회 - 정상 응답")
    fun getPublicProfileSuccess() {
        val member = memberRepository.save(
            Member.createLocalMember(
                email = "public-profile@test.com",
                passwordHash = "dummy-password",
                nickname = "publicUser",
            ),
        )
        val category = categoryRepository.save(Category("public-profile-category"))

        postRepository.save(Post.create(member, category, "공개 글 1", "내용 1"))
        postRepository.save(Post.create(member, category, "공개 글 2", "내용 2"))
        val deletedPost = postRepository.save(Post.create(member, category, "삭제된 글", "내용 3"))

        deletedPost.delete()
        postRepository.flush()

        mvc.perform(MockMvcRequestBuilders.get("/api/users/{userId}/profile", member.userId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPublicProfile"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(member.userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("publicUser"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.posts.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.posts[?(@.title == '공개 글 1')]").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.posts[?(@.title == '공개 글 2')]").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.posts[?(@.title == '삭제된 글')]").isEmpty)
    }

    @Test
    @DisplayName("공개 프로필 조회 - 존재하지 않는 사용자")
    fun getPublicProfileMemberNotFound() {
        val notExistsUserId = 999999L

        mvc.perform(MockMvcRequestBuilders.get("/api/users/{userId}/profile", notExistsUserId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_404_NOT_FOUND"))
    }

    @Test
    @DisplayName("공개 프로필 조회 - 게시글 없는 사용자")
    fun getPublicProfileEmptyPosts() {
        val member = memberRepository.save(
            Member.createLocalMember(
                email = "empty-posts@test.com",
                passwordHash = "dummy-password",
                nickname = "noPostsUser",
            ),
        )

        mvc.perform(MockMvcRequestBuilders.get("/api/users/{userId}/profile", member.userId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(member.userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("noPostsUser"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.posts.length()").value(0))
    }
}