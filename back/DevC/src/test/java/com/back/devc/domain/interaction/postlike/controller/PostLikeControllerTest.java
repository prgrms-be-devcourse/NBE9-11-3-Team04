package com.back.devc.domain.interaction.postlike.controller;

import com.back.devc.domain.interaction.postLike.controller.PostLikeController;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery;
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand;
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.global.response.successCode.PostLikeSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import com.back.devc.global.security.jwt.JwtPrincipalHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.MethodParameter;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PostLikeController 테스트")
class PostLikeControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostLikeService postLikeService = new PostLikeService(null, null, null, null, null) {

            @Override
            public PostLikeResponse createLike(PostLikeCommand command) {
                return new PostLikeResponse(
                        command.postId(),
                        true,
                        5,
                        PostLikeSuccessCode.POST_LIKE_CREATED.getMessage()
                );
            }

            @Override
            public PostLikeResponse cancelLike(PostLikeCommand command) {
                return new PostLikeResponse(
                        command.postId(),
                        false,
                        4,
                        PostLikeSuccessCode.POST_LIKE_CANCELED.getMessage()
                );
            }

            @Override
            public List<LikedPostResponse> getLikedPosts(LikedPostsQuery query) {
                return List.of(
                        new LikedPostResponse(
                                10L,
                                "테스트 게시글",
                                "작성자",
                                5,
                                2,
                                100,
                                LocalDateTime.of(2026, 5, 15, 10, 0),
                                true,
                                false
                        )
                );
            }
        };

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostLikeController(postLikeService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .build();
    }

    @Test
    @DisplayName("게시글 좋아요 추가 성공")
    void createLike_success() throws Exception {
        try (MockedStatic<JwtPrincipalHelper> mockedStatic = mockStatic(JwtPrincipalHelper.class)) {
            mockedStatic.when(() -> JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal.class)))
                    .thenReturn(1L);

            mockMvc.perform(post("/api/posts/10/likes")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code")
                            .value(PostLikeSuccessCode.POST_LIKE_CREATED.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(PostLikeSuccessCode.POST_LIKE_CREATED.getMessage()))
                    .andExpect(jsonPath("$.data.postId").value(10L))
                    .andExpect(jsonPath("$.data.liked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(5))
                    .andExpect(jsonPath("$.data.message")
                            .value(PostLikeSuccessCode.POST_LIKE_CREATED.getMessage()));
        }
    }

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    void cancelLike_success() throws Exception {
        try (MockedStatic<JwtPrincipalHelper> mockedStatic = mockStatic(JwtPrincipalHelper.class)) {
            mockedStatic.when(() -> JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal.class)))
                    .thenReturn(1L);

            mockMvc.perform(delete("/api/posts/10/likes")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code")
                            .value(PostLikeSuccessCode.POST_LIKE_CANCELED.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(PostLikeSuccessCode.POST_LIKE_CANCELED.getMessage()))
                    .andExpect(jsonPath("$.data.postId").value(10L))
                    .andExpect(jsonPath("$.data.liked").value(false))
                    .andExpect(jsonPath("$.data.likeCount").value(4))
                    .andExpect(jsonPath("$.data.message")
                            .value(PostLikeSuccessCode.POST_LIKE_CANCELED.getMessage()));
        }
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록 조회 성공")
    void getLikedPosts_success() throws Exception {
        try (MockedStatic<JwtPrincipalHelper> mockedStatic = mockStatic(JwtPrincipalHelper.class)) {
            mockedStatic.when(() -> JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal.class)))
                    .thenReturn(1L);

            mockMvc.perform(get("/api/users/me/likes")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code")
                            .value(PostLikeSuccessCode.LIKED_POSTS_FETCHED.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(PostLikeSuccessCode.LIKED_POSTS_FETCHED.getMessage()))
                    .andExpect(jsonPath("$.data[0].postId").value(10L))
                    .andExpect(jsonPath("$.data[0].title").value("테스트 게시글"))
                    .andExpect(jsonPath("$.data[0].authorNickname").value("작성자"))
                    .andExpect(jsonPath("$.data[0].likeCount").value(5))
                    .andExpect(jsonPath("$.data[0].commentCount").value(2))
                    .andExpect(jsonPath("$.data[0].viewCount").value(100))
                    .andExpect(jsonPath("$.data[0].liked").value(true))
                    .andExpect(jsonPath("$.data[0].bookmarked").value(false));
        }
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
            return mock(JwtPrincipal.class);
        }
    }
}