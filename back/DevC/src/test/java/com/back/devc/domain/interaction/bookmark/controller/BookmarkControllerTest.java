package com.back.devc.domain.interaction.bookmark.controller;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.response.successCode.BookmarkSuccessCode;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookmarkController 테스트")
class BookmarkControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BookmarkService bookmarkService = new BookmarkService(
                mock(BookmarkRepository.class),
                mock(PostLikeRepository.class),
                mock(MemberRepository.class),
                mock(PostRepository.class)
        ) {

            @Override
            public BookmarkResponse createBookmark(BookmarkCreateCommand command) {
                return new BookmarkResponse(
                        command.getPostId(),
                        true
                );
            }

            @Override
            public BookmarkResponse cancelBookmark(BookmarkDeleteCommand command) {
                return new BookmarkResponse(
                        command.getPostId(),
                        false
                );
            }
        };

        mockMvc = MockMvcBuilders
                .standaloneSetup(new BookmarkController(bookmarkService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .build();
    }

    @Test
    @DisplayName("게시글 북마크 추가 성공")
    void createBookmark_success() throws Exception {
        try (MockedStatic<JwtPrincipalHelper> mockedStatic = mockStatic(JwtPrincipalHelper.class)) {
            mockedStatic.when(() -> JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal.class)))
                    .thenReturn(1L);

            mockMvc.perform(post("/api/posts/10/bookmarks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code")
                            .value(BookmarkSuccessCode.BOOKMARK_201_CREATE.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(BookmarkSuccessCode.BOOKMARK_201_CREATE.getMessage()))
                    .andExpect(jsonPath("$.data.postId").value(10L))
                    .andExpect(jsonPath("$.data.bookmarked").value(true));
        }
    }

    @Test
    @DisplayName("게시글 북마크 취소 성공")
    void cancelBookmark_success() throws Exception {
        try (MockedStatic<JwtPrincipalHelper> mockedStatic = mockStatic(JwtPrincipalHelper.class)) {
            mockedStatic.when(() -> JwtPrincipalHelper.getAuthenticatedUserId(any(JwtPrincipal.class)))
                    .thenReturn(1L);

            mockMvc.perform(delete("/api/posts/10/bookmarks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code")
                            .value(BookmarkSuccessCode.BOOKMARK_200_DELETE.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(BookmarkSuccessCode.BOOKMARK_200_DELETE.getMessage()))
                    .andExpect(jsonPath("$.data.postId").value(10L))
                    .andExpect(jsonPath("$.data.bookmarked").value(false));
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