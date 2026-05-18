package com.back.devc.domain.member.mypage.controller;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.member.mypage.dto.MyCommentResponse;
import com.back.devc.domain.member.mypage.dto.MyPostResponse;
import com.back.devc.domain.member.mypage.dto.MyProfileResponse;
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest;
import com.back.devc.domain.member.mypage.service.MypageService;
import com.back.devc.global.response.PageResponse;
import com.back.devc.global.response.successCode.MypageSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MypageController 테스트")
class MypageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MypageService mypageService = mock(MypageService.class);

        MyProfileResponse profileResponse = new MyProfileResponse(
                1L,
                "test@test.com",
                "기존닉네임"
        );

        MyPostResponse post = new MyPostResponse(
                10L,
                "내 게시글",
                5L,
                2L,
                100L,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                true,
                false
        );

        MyCommentResponse comment = new MyCommentResponse(
                100L,
                10L,
                "게시글 제목",
                "댓글 내용",
                LocalDateTime.of(2026, 5, 15, 10, 0)
        );

        LikedPostResponse likedPost = new LikedPostResponse(
                10L,
                "좋아요한 게시글",
                "작성자",
                5L,
                2L,
                100L,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                true,
                false
        );

        BookmarkedPostResponse bookmarkedPost = new BookmarkedPostResponse(
                10L,
                "북마크한 게시글",
                "작성자",
                100L,
                5L,
                2L,
                100L,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                false,
                true
        );

        MyProfileResponse updatedProfileResponse = new MyProfileResponse(
                1L,
                "test@test.com",
                "변경닉네임"
        );

        when(mypageService.getMyProfile(anyLong()))
                .thenReturn(profileResponse);

        when(mypageService.getMyPosts(anyLong(), any(Pageable.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1)
                ));

        when(mypageService.getMyComments(anyLong(), any(Pageable.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(comment), PageRequest.of(0, 10), 1)
                ));

        when(mypageService.getMyLikedPosts(anyLong(), any(Pageable.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(likedPost), PageRequest.of(0, 10), 1)
                ));

        when(mypageService.getMyBookmarkedPosts(anyLong(), any(Pageable.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(bookmarkedPost), PageRequest.of(0, 10), 1)
                ));

        when(mypageService.updateMyProfile(anyLong(), any(UpdateMyProfileRequest.class)))
                .thenReturn(updatedProfileResponse);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MypageController(mypageService))
                .setCustomArgumentResolvers(
                        new TestAuthenticationPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_success() throws Exception {
        mockMvc.perform(get("/api/mypage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_PROFILE_FETCH.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_PROFILE_FETCH.getMessage()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("기존닉네임"));
    }

    @Test
    @DisplayName("내 게시글 목록 조회 성공")
    void getMyPosts_success() throws Exception {
        mockMvc.perform(get("/api/mypage/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_POSTS_FETCH.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_POSTS_FETCH.getMessage()))
                .andExpect(jsonPath("$.data.content[0].postId").value(10L))
                .andExpect(jsonPath("$.data.content[0].title").value("내 게시글"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(5))
                .andExpect(jsonPath("$.data.content[0].commentCount").value(2))
                .andExpect(jsonPath("$.data.content[0].viewCount").value(100))
                .andExpect(jsonPath("$.data.content[0].liked").value(true))
                .andExpect(jsonPath("$.data.content[0].bookmarked").value(false));
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMyComments_success() throws Exception {
        mockMvc.perform(get("/api/mypage/comments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH.getMessage()))
                .andExpect(jsonPath("$.data.content[0].commentId").value(100L))
                .andExpect(jsonPath("$.data.content[0].postId").value(10L))
                .andExpect(jsonPath("$.data.content[0].postTitle").value("게시글 제목"))
                .andExpect(jsonPath("$.data.content[0].content").value("댓글 내용"));
    }

    @Test
    @DisplayName("내 좋아요 게시글 목록 조회 성공")
    void getMyLikedPosts_success() throws Exception {
        mockMvc.perform(get("/api/mypage/likes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_LIKES_FETCH.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_LIKES_FETCH.getMessage()))
                .andExpect(jsonPath("$.data.content[0].postId").value(10L))
                .andExpect(jsonPath("$.data.content[0].title").value("좋아요한 게시글"))
                .andExpect(jsonPath("$.data.content[0].authorNickname").value("작성자"))
                .andExpect(jsonPath("$.data.content[0].liked").value(true))
                .andExpect(jsonPath("$.data.content[0].bookmarked").value(false));
    }

    @Test
    @DisplayName("내 북마크 게시글 목록 조회 성공")
    void getMyBookmarkedPosts_success() throws Exception {
        mockMvc.perform(get("/api/mypage/bookmarks")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH.getMessage()))
                .andExpect(jsonPath("$.data.content[0].postId").value(10L))
                .andExpect(jsonPath("$.data.content[0].title").value("북마크한 게시글"))
                .andExpect(jsonPath("$.data.content[0].authorNickname").value("작성자"))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(100L))
                .andExpect(jsonPath("$.data.content[0].liked").value(false))
                .andExpect(jsonPath("$.data.content[0].bookmarked").value(true));
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    void updateMyProfile_success() throws Exception {
        mockMvc.perform(patch("/api/mypage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "변경닉네임"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE.getMessage()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("변경닉네임"));
    }

    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(JwtPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            JwtPrincipal principal = mock(JwtPrincipal.class);
            when(principal.userId()).thenReturn(1L);

            return principal;
        }
    }
}
