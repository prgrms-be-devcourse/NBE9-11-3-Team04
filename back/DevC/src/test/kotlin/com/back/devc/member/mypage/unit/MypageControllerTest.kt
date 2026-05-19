package com.back.devc.domain.member.mypage.unit

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.member.mypage.controller.MypageController
import com.back.devc.domain.member.mypage.dto.MyCommentResponse
import com.back.devc.domain.member.mypage.dto.MyPostResponse
import com.back.devc.domain.member.mypage.dto.MyProfileResponse
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest
import com.back.devc.domain.member.mypage.service.MypageService
import com.back.devc.global.response.PageResponse
import com.back.devc.global.response.successCode.MypageSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.LocalDateTime

@DisplayName("MypageController 테스트")
class MypageControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val mypageService = mock<MypageService>()

        val profileResponse = MyProfileResponse(
            userId = 1L,
            email = "test@test.com",
            nickname = "기존닉네임",
        )

        val post = MyPostResponse(
            postId = 10L,
            title = "내 게시글",
            likeCount = 5L,
            commentCount = 2L,
            viewCount = 100L,
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
            liked = true,
            bookmarked = false,
        )

        val comment = MyCommentResponse(
            commentId = 100L,
            postId = 10L,
            postTitle = "게시글 제목",
            content = "댓글 내용",
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
        )

        val likedPost = LikedPostResponse(
            postId = 10L,
            title = "좋아요한 게시글",
            authorNickname = "작성자",
            likeCount = 5L,
            commentCount = 2L,
            viewCount = 100L,
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
            liked = true,
            bookmarked = false,
        )

        val bookmarkedPost = BookmarkedPostResponse(
            postId = 10L,
            title = "북마크한 게시글",
            authorNickname = "작성자",
            categoryId = 100L,
            likeCount = 5L,
            commentCount = 2L,
            viewCount = 100L,
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
            liked = false,
            bookmarked = true,
        )

        val updatedProfileResponse = MyProfileResponse(
            userId = 1L,
            email = "test@test.com",
            nickname = "변경닉네임",
        )

        whenever(mypageService.getMyProfile(any<Long>()))
            .thenReturn(profileResponse)

        whenever(mypageService.getMyPosts(any<Long>(), any<Pageable>()))
            .thenReturn(
                PageResponse.from(
                    PageImpl(listOf(post), PageRequest.of(0, 10), 1)
                )
            )

        whenever(mypageService.getMyComments(any<Long>(), any<Pageable>()))
            .thenReturn(
                PageResponse.from(
                    PageImpl(listOf(comment), PageRequest.of(0, 10), 1)
                )
            )

        whenever(mypageService.getMyLikedPosts(any<Long>(), any<Pageable>()))
            .thenReturn(
                PageResponse.from(
                    PageImpl(listOf(likedPost), PageRequest.of(0, 10), 1)
                )
            )

        whenever(mypageService.getMyBookmarkedPosts(any<Long>(), any<Pageable>()))
            .thenReturn(
                PageResponse.from(
                    PageImpl(listOf(bookmarkedPost), PageRequest.of(0, 10), 1)
                )
            )

        whenever(mypageService.updateMyProfile(any<Long>(), any<UpdateMyProfileRequest>()))
            .thenReturn(updatedProfileResponse)

        mockMvc = MockMvcBuilders
            .standaloneSetup(MypageController(mypageService))
            .setCustomArgumentResolvers(
                TestAuthenticationPrincipalResolver(),
                PageableHandlerMethodArgumentResolver(),
            )
            .build()
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    fun getMyProfileSuccess() {
        mockMvc.perform(get("/api/mypage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_PROFILE_FETCH.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_PROFILE_FETCH.message))
            .andExpect(jsonPath("$.data.userId").value(1L))
            .andExpect(jsonPath("$.data.email").value("test@test.com"))
            .andExpect(jsonPath("$.data.nickname").value("기존닉네임"))
    }

    @Test
    @DisplayName("내 게시글 목록 조회 성공")
    fun getMyPostsSuccess() {
        mockMvc.perform(
            get("/api/mypage/posts")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_POSTS_FETCH.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_POSTS_FETCH.message))
            .andExpect(jsonPath("$.data.content[0].postId").value(10L))
            .andExpect(jsonPath("$.data.content[0].title").value("내 게시글"))
            .andExpect(jsonPath("$.data.content[0].likeCount").value(5))
            .andExpect(jsonPath("$.data.content[0].commentCount").value(2))
            .andExpect(jsonPath("$.data.content[0].viewCount").value(100))
            .andExpect(jsonPath("$.data.content[0].liked").value(true))
            .andExpect(jsonPath("$.data.content[0].bookmarked").value(false))
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    fun getMyCommentsSuccess() {
        mockMvc.perform(
            get("/api/mypage/comments")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH.message))
            .andExpect(jsonPath("$.data.content[0].commentId").value(100L))
            .andExpect(jsonPath("$.data.content[0].postId").value(10L))
            .andExpect(jsonPath("$.data.content[0].postTitle").value("게시글 제목"))
            .andExpect(jsonPath("$.data.content[0].content").value("댓글 내용"))
    }

    @Test
    @DisplayName("내 좋아요 게시글 목록 조회 성공")
    fun getMyLikedPostsSuccess() {
        mockMvc.perform(
            get("/api/mypage/likes")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_LIKES_FETCH.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_LIKES_FETCH.message))
            .andExpect(jsonPath("$.data.content[0].postId").value(10L))
            .andExpect(jsonPath("$.data.content[0].title").value("좋아요한 게시글"))
            .andExpect(jsonPath("$.data.content[0].authorNickname").value("작성자"))
            .andExpect(jsonPath("$.data.content[0].liked").value(true))
            .andExpect(jsonPath("$.data.content[0].bookmarked").value(false))
    }

    @Test
    @DisplayName("내 북마크 게시글 목록 조회 성공")
    fun getMyBookmarkedPostsSuccess() {
        mockMvc.perform(
            get("/api/mypage/bookmarks")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH.message))
            .andExpect(jsonPath("$.data.content[0].postId").value(10L))
            .andExpect(jsonPath("$.data.content[0].title").value("북마크한 게시글"))
            .andExpect(jsonPath("$.data.content[0].authorNickname").value("작성자"))
            .andExpect(jsonPath("$.data.content[0].categoryId").value(100L))
            .andExpect(jsonPath("$.data.content[0].liked").value(false))
            .andExpect(jsonPath("$.data.content[0].bookmarked").value(true))
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    fun updateMyProfileSuccess() {
        mockMvc.perform(
            patch("/api/mypage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nickname": "변경닉네임"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE.code))
            .andExpect(jsonPath("$.message").value(MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE.message))
            .andExpect(jsonPath("$.data.userId").value(1L))
            .andExpect(jsonPath("$.data.email").value("test@test.com"))
            .andExpect(jsonPath("$.data.nickname").value("변경닉네임"))
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
            val principal = mock<JwtPrincipal>()
            whenever(principal.userId).thenReturn(1L)

            return principal
        }
    }
}