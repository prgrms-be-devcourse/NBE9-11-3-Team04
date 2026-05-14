package com.back.devc.domain.interaction;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    private Member loginMember;
    private Member authorMember;
    private Category category;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        loginMember = memberRepository.save(
                Member.createLocalMember(
                        "test@test.com",
                        "password123!",
                        "tester"
                )
        );

        authorMember = memberRepository.save(
                Member.createLocalMember(
                        "author@test.com",
                        "password123!",
                        "author"
                )
        );

        category = categoryRepository.save(new Category("테스트 자유"));
    }

    private void setAuthentication(Member member) {
        JwtPrincipal principal = new JwtPrincipal(
                member.getUserId(),
                member.getEmail(),
                "USER"
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private Post createPost(Member member, String title) {
        return postRepository.save(new Post(member, category, title, "테스트 내용"));
    }

    @Test
    @DisplayName("좋아요 생성 성공")
    void createLike_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/likes", post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POST_LIKE_CREATED"))
                .andExpect(jsonPath("$.message").value("좋아요가 추가되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.message").value("좋아요가 추가되었습니다."));
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    void deleteLike_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/likes", post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk());

        mvc.perform(delete("/api/posts/{postId}/likes", post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POST_LIKE_CANCELED"))
                .andExpect(jsonPath("$.message").value("좋아요가 취소되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.message").value("좋아요가 취소되었습니다."));
    }

    @Test
    @DisplayName("북마크 생성 성공")
    void createBookmark_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/bookmarks", post.getPostId()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BOOKMARK_201_CREATE"))
                .andExpect(jsonPath("$.message").value("북마크가 추가되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("북마크 취소 성공")
    void deleteBookmark_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/bookmarks", post.getPostId()))
                .andDo(print())
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/posts/{postId}/bookmarks", post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOOKMARK_200_DELETE"))
                .andExpect(jsonPath("$.message").value("북마크가 취소되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.bookmarked").value(false));
    }

    @Test
    @DisplayName("마이페이지 프로필 조회 성공")
    void mypage_profile_success() throws Exception {
        setAuthentication(loginMember);

        mvc.perform(get("/api/mypage"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYPAGE_200_PROFILE_FETCH"))
                .andExpect(jsonPath("$.message").value("내 프로필을 조회했습니다."))
                .andExpect(jsonPath("$.data.userId").value(loginMember.getUserId()))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"));
    }

    @Test
    @DisplayName("마이페이지 게시글 조회 성공")
    void mypage_posts_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(loginMember, "테스트 제목");

        mvc.perform(get("/api/mypage/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYPAGE_200_POSTS_FETCH"))
                .andExpect(jsonPath("$.message").value("내 게시글 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data[0].title").value("테스트 제목"));
    }

    @Test
    @DisplayName("마이페이지 좋아요 목록 조회 성공")
    void mypage_likes_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/likes", post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk());

        mvc.perform(get("/api/users/me/likes"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LIKED_POSTS_FETCHED"))
                .andExpect(jsonPath("$.message").value("좋아요한 게시글 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data[0].title").value("테스트 제목"));
    }

    @Test
    @DisplayName("마이페이지 북마크 목록 조회 성공")
    void mypage_bookmarks_success() throws Exception {
        setAuthentication(loginMember);
        Post post = createPost(authorMember, "테스트 제목");

        mvc.perform(post("/api/posts/{postId}/bookmarks", post.getPostId()))
                .andDo(print())
                .andExpect(status().isCreated());

        mvc.perform(get("/api/mypage/bookmarks"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].postId").value(post.getPostId()));
    }

    @Test
    @DisplayName("마이페이지 프로필 수정 성공")
    void updateMyProfile_success() throws Exception {
        setAuthentication(loginMember);

        mvc.perform(
                        patch("/api/mypage")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "nickname": "newTester"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYPAGE_200_PROFILE_UPDATE"))
                .andExpect(jsonPath("$.message").value("내 프로필을 수정했습니다."))
                .andExpect(jsonPath("$.data.userId").value(loginMember.getUserId()))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("newTester"));
    }
}