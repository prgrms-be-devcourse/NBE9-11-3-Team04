package com.back.devc.domain.interaction.postlike.unit

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.postLike.controller.PostLikeController
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.interaction.postLike.service.PostLikeService
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.response.successCode.PostLikeSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.LocalDateTime

@DisplayName("PostLikeController 테스트")
class PostLikeControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val postLikeService = object : PostLikeService(
            postLikeRepository = mock(PostLikeRepository::class.java),
            bookmarkRepository = mock(BookmarkRepository::class.java),
            memberRepository = mock(MemberRepository::class.java),
            postRepository = mock(PostRepository::class.java),
            notificationService = mock(NotificationService::class.java),
        ) {
            override fun createLike(command: PostLikeCommand): PostLikeResponse {
                return PostLikeResponse(
                    postId = command.postId,
                    liked = true,
                    likeCount = 5L,
                    message = PostLikeSuccessCode.POST_LIKE_CREATED.message,
                )
            }

            override fun cancelLike(command: PostLikeCommand): PostLikeResponse {
                return PostLikeResponse(
                    postId = command.postId,
                    liked = false,
                    likeCount = 4L,
                    message = PostLikeSuccessCode.POST_LIKE_CANCELED.message,
                )
            }

            override fun getLikedPosts(query: LikedPostsQuery): List<LikedPostResponse> {
                return listOf(
                    LikedPostResponse(
                        postId = 10L,
                        title = "테스트 게시글",
                        authorNickname = "작성자",
                        likeCount = 5L,
                        commentCount = 2L,
                        viewCount = 100L,
                        createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
                        liked = true,
                        bookmarked = false,
                    )
                )
            }
        }

        mockMvc = MockMvcBuilders
            .standaloneSetup(PostLikeController(postLikeService))
            .setCustomArgumentResolvers(TestAuthenticationPrincipalResolver())
            .build()
    }

    @Test
    @DisplayName("게시글 좋아요 추가 성공")
    fun createLikeSuccess() {
        mockStatic(JwtPrincipalHelper::class.java).use { mockedStatic ->
            mockedStatic.`when`<Long> {
                JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal::class.java))
            }.thenReturn(1L)

            mockMvc.perform(
                post("/api/posts/10/likes")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath("$.code")
                        .value(PostLikeSuccessCode.POST_LIKE_CREATED.code)
                )
                .andExpect(
                    jsonPath("$.message")
                        .value(PostLikeSuccessCode.POST_LIKE_CREATED.message)
                )
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5))
                .andExpect(
                    jsonPath("$.data.message")
                        .value(PostLikeSuccessCode.POST_LIKE_CREATED.message)
                )
        }
    }

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    fun cancelLikeSuccess() {
        mockStatic(JwtPrincipalHelper::class.java).use { mockedStatic ->
            mockedStatic.`when`<Long> {
                JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal::class.java))
            }.thenReturn(1L)

            mockMvc.perform(
                delete("/api/posts/10/likes")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath("$.code")
                        .value(PostLikeSuccessCode.POST_LIKE_CANCELED.code)
                )
                .andExpect(
                    jsonPath("$.message")
                        .value(PostLikeSuccessCode.POST_LIKE_CANCELED.message)
                )
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(4))
                .andExpect(
                    jsonPath("$.data.message")
                        .value(PostLikeSuccessCode.POST_LIKE_CANCELED.message)
                )
        }
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록 조회 성공")
    fun getLikedPostsSuccess() {
        mockStatic(JwtPrincipalHelper::class.java).use { mockedStatic ->
            mockedStatic.`when`<Long> {
                JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal::class.java))
            }.thenReturn(1L)

            mockMvc.perform(
                get("/api/users/me/likes")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath("$.code")
                        .value(PostLikeSuccessCode.LIKED_POSTS_FETCHED.code)
                )
                .andExpect(
                    jsonPath("$.message")
                        .value(PostLikeSuccessCode.LIKED_POSTS_FETCHED.message)
                )
                .andExpect(jsonPath("$.data[0].postId").value(10L))
                .andExpect(jsonPath("$.data[0].title").value("테스트 게시글"))
                .andExpect(jsonPath("$.data[0].authorNickname").value("작성자"))
                .andExpect(jsonPath("$.data[0].likeCount").value(5))
                .andExpect(jsonPath("$.data[0].commentCount").value(2))
                .andExpect(jsonPath("$.data[0].viewCount").value(100))
                .andExpect(jsonPath("$.data[0].liked").value(true))
                .andExpect(jsonPath("$.data[0].bookmarked").value(false))
        }
    }

    private class TestAuthenticationPrincipalResolver : HandlerMethodArgumentResolver {

        override fun supportsParameter(parameter: MethodParameter): Boolean {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java) &&
                    parameter.parameterType == JwtPrincipal::class.java
        }

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?,
        ): Any {
            return mock(JwtPrincipal::class.java)
        }
    }
}