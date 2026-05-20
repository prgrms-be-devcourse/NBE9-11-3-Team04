package com.back.devc.domain.interaction.bookmark.unit

import com.back.devc.domain.interaction.bookmark.controller.BookmarkController
import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.bookmark.service.BookmarkService
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.response.successCode.BookmarkSuccessCode
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@DisplayName("BookmarkController 테스트")
class BookmarkControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val bookmarkService = object : BookmarkService(
            bookmarkRepository = mock(BookmarkRepository::class.java),
            postLikeRepository = mock(PostLikeRepository::class.java),
            memberRepository = mock(MemberRepository::class.java),
            postRepository = mock(PostRepository::class.java),
        ) {
            override fun createBookmark(command: BookmarkCreateCommand): BookmarkResponse {
                return BookmarkResponse(
                    postId = command.postId,
                    bookmarked = true,
                )
            }

            override fun cancelBookmark(command: BookmarkDeleteCommand): BookmarkResponse {
                return BookmarkResponse(
                    postId = command.postId,
                    bookmarked = false,
                )
            }
        }

        mockMvc = MockMvcBuilders
            .standaloneSetup(BookmarkController(bookmarkService))
            .setCustomArgumentResolvers(TestAuthenticationPrincipalResolver())
            .build()
    }

    @Test
    @DisplayName("게시글 북마크 추가 성공")
    fun createBookmarkSuccess() {
        mockStatic(JwtPrincipalHelper::class.java).use { mockedStatic ->
            mockedStatic.`when`<Long> {
                JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal::class.java))
            }.thenReturn(1L)

            mockMvc.perform(
                post("/api/posts/10/bookmarks")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isCreated)
                .andExpect(
                    jsonPath("$.code")
                        .value(BookmarkSuccessCode.BOOKMARK_201_CREATE.code)
                )
                .andExpect(
                    jsonPath("$.message")
                        .value(BookmarkSuccessCode.BOOKMARK_201_CREATE.message)
                )
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.bookmarked").value(true))
        }
    }

    @Test
    @DisplayName("게시글 북마크 취소 성공")
    fun cancelBookmarkSuccess() {
        mockStatic(JwtPrincipalHelper::class.java).use { mockedStatic ->
            mockedStatic.`when`<Long> {
                JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal::class.java))
            }.thenReturn(1L)

            mockMvc.perform(
                delete("/api/posts/10/bookmarks")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath("$.code")
                        .value(BookmarkSuccessCode.BOOKMARK_200_DELETE.code)
                )
                .andExpect(
                    jsonPath("$.message")
                        .value(BookmarkSuccessCode.BOOKMARK_200_DELETE.message)
                )
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.bookmarked").value(false))
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