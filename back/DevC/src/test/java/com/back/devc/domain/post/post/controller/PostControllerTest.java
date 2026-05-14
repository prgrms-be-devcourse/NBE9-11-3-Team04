package com.back.devc.domain.post.post.controller;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.dto.PostCreateRequest;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostControllerTest{

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PostRepository postRepository;

    private Member member;
    private Category category;

    // =========================
    // SETUP
    // =========================
    @BeforeEach
    void setUp() {

        member = memberRepository.save(
                Member.createLocalMember(
                        "test@test.com",
                        "password123!",
                        "testUser"
                )
        );

        category = new Category("테스트 자유");
        category = categoryRepository.save(category);
    }

    private void setAuthentication() {
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

    // =========================
    // CREATE
    // =========================
    @Test
    @DisplayName("게시글 생성")
    void t1() throws Exception {

        setAuthentication();
        PostCreateRequest request = new PostCreateRequest(
                "테스트글",
                "테스트내용입니다.",
                category.getCategoryId()
        );

        mvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").exists())
                .andExpect(jsonPath("$.message").value("게시글 작성 성공"));

    }

    // =========================
    // DETAIL
    // =========================
    @Test
    @DisplayName("게시글 상세 조회")
    void t2() throws Exception {

        Post post = postRepository.save(
                new Post(member, category, "테스트3", "테스트3내용")
        );

        mvc.perform(get("/api/posts/" + post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("테스트3"));
    }
    // =========================
    // UPDATE
    // =========================
    @Test
    @DisplayName("게시글 수정")
    void t3() throws Exception {

        setAuthentication();

        Post post = postRepository.save(
                new Post(member, category, "수정 전 제목", "수정 전 내용")
        );

        mvc.perform(
                        put("/api/posts/" + post.getPostId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "수정 후 제목",
                                      "content": "수정 후 내용",
                                      "categoryId": %d
                                    }
                                    """.formatted(category.getCategoryId()))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정 후 제목"));
    }
    // =========================
    // DELETE
    // =========================
    @Test
    @DisplayName("게시글 삭제")
    void t4() throws Exception {

        setAuthentication();

        Post post = postRepository.save(
                new Post(member, category, "title", "content")
        );

        mvc.perform(delete("/api/posts/" + post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 삭제 성공"));

        Post deleted = postRepository.findById(post.getPostId()).orElseThrow();

        assertThat(deleted.isDeleted()).isTrue();
        //삭제 후 isDeleted의 값이 true로 변경됨을 보여줌
    }

    // =========================
    // 최신순, 좋아요순, 조회수순
    // =========================


    @Test
    @DisplayName("게시글 최신순 조회")
    void t5() throws Exception {

        Post post1 = postRepository.save(
                new Post(member, category, "첫번째", "내용1")
        );

        Thread.sleep(10);

        Post post2 = postRepository.save(
                new Post(member, category, "두번째", "내용2")
        );

        mvc.perform(get("/api/posts")
                        .param("sort", "LATEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("두번째"))
                .andExpect(jsonPath("$.data.content[1].title").value("첫번째"));
    }

    @Test
    @DisplayName("게시글 좋아요순 조회, 만약 좋아요 개수가 같은경우 최신순으로 보여줌")
    void t6() throws Exception {

        // given
        Post post1 = postRepository.save(new Post(member, category, "제목1", "내용1"));
        Thread.sleep(10); // createdAt 차이
        Post post2 = postRepository.save(new Post(member, category, "제목2", "내용2"));
        Thread.sleep(10);
        Post post3 = postRepository.save(new Post(member, category, "제목3", "내용3"));

        // 좋아요 증가
        for (int i = 0; i < 5; i++) post1.increaseLikeCount(); //제목1
        for (int i = 0; i < 10; i++) post2.increaseLikeCount(); // 제목2
        for (int i = 0; i < 5; i++) post3.increaseLikeCount(); //제목3

        postRepository.flush();

        // when
        ResultActions result = mvc.perform(get("/api/posts")
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10")
        );

        // then
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("제목2")) // 좋아요 10
                .andExpect(jsonPath("$.data.content[1].title").value("제목3")) // 좋아요 5 + 최신
                .andExpect(jsonPath("$.data.content[2].title").value("제목1")); // 좋아요 5 + 오래됨
    }


    @Test
    @DisplayName("게시글 조회수 순서 조회, 만약 조회수 개수가 같은경우 최신순으로 보여준다")
    void t7() throws Exception {

        // given
        Post post1 = postRepository.save(new Post(member, category, "제목1", "내용1"));
        Thread.sleep(10);
        Post post2 = postRepository.save(new Post(member, category, "제목2", "내용2"));
        Thread.sleep(10);
        Post post3 = postRepository.save(new Post(member, category, "제목3", "내용3"));

        // 조회수 증가
        for (int i = 0; i < 5; i++) post1.increaseViewCount();
        for (int i = 0; i < 10; i++) post2.increaseViewCount();
        for (int i = 0; i < 5; i++) post3.increaseViewCount();

        postRepository.flush();

        // when
        ResultActions result = mvc.perform(get("/api/posts")
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10")
        );

        // then
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("제목2")) // 조회수 10
                .andExpect(jsonPath("$.data.content[1].title").value("제목3")) // 조회수 5 + 최신
                .andExpect(jsonPath("$.data.content[2].title").value("제목1")); // 조회수 5 + 오래됨
    }


    @Test
    @DisplayName("게시글 카테고리별 조회")
    void t8() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("테스트 공지"));

        // category1 게시글
        postRepository.save(new Post(member, category, "자유1", "내용1"));
        postRepository.save(new Post(member, category, "자유2", "내용2"));

        // category2 게시글
        postRepository.save(new Post(member, category2, "공지1", "내용3"));

        // when & then
        mvc.perform(get("/api/posts")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                // category1 글만 2개 나와야 함
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(category.getCategoryId()))
                .andExpect(jsonPath("$.data.content[1].categoryId").value(category.getCategoryId()));
    }

    @Test
    @DisplayName("카테고리 + 최신순 조회")
    void t9() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(new Post(member, category, "자유1", "내용1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(new Post(member, category, "자유2", "내용2"));

        // 다른 카테고리
        postRepository.save(new Post(member, category2, "공지1", "내용3"));

        // when & then
        mvc.perform(get("/api/posts")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("sort", "LATEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                // 최신순 → p2 먼저
                .andExpect(jsonPath("$.data.content[0].title").value("자유2"))
                .andExpect(jsonPath("$.data.content[1].title").value("자유1"));
    }

    @Test
    @DisplayName("카테고리 + 좋아요순 조회")
    void t10() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(new Post(member, category, "자유1", "내용1"));
        Post p2 = postRepository.save(new Post(member, category, "자유2", "내용2"));

        // 좋아요 차이
        for (int i = 0; i < 5; i++) p1.increaseLikeCount();
        for (int i = 0; i < 10; i++) p2.increaseLikeCount();

        // 다른 카테고리
        Post other = postRepository.save(new Post(member, category2, "공지1", "내용3"));
        for (int i = 0; i < 100; i++) other.increaseLikeCount();

        postRepository.flush();

        // when & then
        mvc.perform(get("/api/posts")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("sort", "LIKES")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                // 좋아요 많은 p2 먼저
                .andExpect(jsonPath("$.data.content[0].title").value("자유2"))
                .andExpect(jsonPath("$.data.content[1].title").value("자유1"));
    }

    @Test
    @DisplayName("카테고리 + 조회수순 조회")
    void t11() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(new Post(member, category, "자유1", "내용1"));
        Post p2 = postRepository.save(new Post(member, category, "자유2", "내용2"));

        // 조회수 차이
        for (int i = 0; i < 5; i++) p1.increaseViewCount();
        for (int i = 0; i < 10; i++) p2.increaseViewCount();

        // 다른 카테고리
        Post other = postRepository.save(new Post(member, category2, "공지1", "내용3"));
        for (int i = 0; i < 100; i++) other.increaseViewCount();

        postRepository.flush();

        // when & then
        mvc.perform(get("/api/posts")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("sort", "VIEWS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                // 조회수 많은 p2 먼저
                .andExpect(jsonPath("$.data.content[0].title").value("자유2"))
                .andExpect(jsonPath("$.data.content[1].title").value("자유1"));
    }

    // =========================
    // 제목, 내용, 제목+내용 검색
    // =========================

    @Test
    @DisplayName("게시글 제목 검색")
    void t12() throws Exception {

        postRepository.save(new Post(member, category, "스프링 공부", "내용1"));
        postRepository.save(new Post(member, category, "자바 공부", "내용2"));
        postRepository.save(new Post(member, category, "리액트 공부", "내용3"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 공부"));
    }

    @Test
    @DisplayName("게시글 내용 검색")
    void t13() throws Exception {

        postRepository.save(new Post(member, category, "글1", "스프링부트 강의"));
        postRepository.save(new Post(member, category, "글2", "자바 강의"));
        postRepository.save(new Post(member, category, "글3", "스프링 핵심"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "CONTENT")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("게시글 제목 + 내용 검색")
    void t14() throws Exception {

        postRepository.save(new Post(member, category, "스프링", "자바 내용"));
        postRepository.save(new Post(member, category, "자바", "스프링 내용"));
        postRepository.save(new Post(member, category, "리액트", "프론트"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE_OR_CONTENT")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("제목 검색 + 최신순")
    void t15() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 1", "내용1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(new Post(member, category, "스프링 2", "내용2"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE")
                        .param("sort", "LATEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 2")); // p2
    }

    @Test
    @DisplayName("제목 검색 + 좋아요순")
    void t16() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 1", "내용1"));
        Post p2 = postRepository.save(new Post(member, category, "스프링 2", "내용2"));

        for (int i = 0; i < 10; i++) p1.increaseLikeCount(); // p1번의 좋아요를 더 많게 작성
        for (int i = 0; i < 5; i++) p2.increaseLikeCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE")
                        .param("sort", "LIKES")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 1")); // p1
    }


    @Test
    @DisplayName("제목 검색 + 조회수순")
    void t17() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 1", "내용1"));
        Post p2 = postRepository.save(new Post(member, category, "스프링 2", "내용2"));
        Post p3 = postRepository.save(new Post(member, category, "스프링 3", "내용3"));

        for (int i = 0; i < 5; i++) p1.increaseViewCount();
        for (int i = 0; i < 10; i++) p2.increaseViewCount();
        for (int i = 0; i < 5; i++) p3.increaseViewCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE")
                        .param("sort", "VIEWS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 2")); // p2
    }


    @Test
    @DisplayName("내용 검색 + 최신순")
    void t18() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "글1", "스프링 1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 2"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "CONTENT")
                        .param("sort", "LATEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("글2")); // p2
    }

    @Test
    @DisplayName("내용 검색 + 좋아요순")
    void t19() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "글1", "스프링 1"));
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 2"));

        for (int i = 0; i < 10; i++) p1.increaseLikeCount();
        for (int i = 0; i < 5; i++) p2.increaseLikeCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "CONTENT")
                        .param("sort", "LIKES")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("글1")); // p1
    }

    @Test
    @DisplayName("내용 검색 + 조회수순")
    void t20() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "글1", "스프링 1"));
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 2"));
        Post p3 = postRepository.save(new Post(member, category, "글3", "스프링 3"));

        for (int i = 0; i < 5; i++) p1.increaseViewCount();
        for (int i = 0; i < 10; i++) p2.increaseViewCount();
        for (int i = 0; i < 5; i++) p3.increaseViewCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "CONTENT")
                        .param("sort", "VIEWS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("글2")); // p2
    }

    @Test
    @DisplayName("제목+내용 검색 + 최신순")
    void t21() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 글1", "내용 A"));
        Thread.sleep(10);
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 내용"));

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE_OR_CONTENT")
                        .param("sort", "LATEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("글2")); // p2
    }

    @Test
    @DisplayName("제목+내용 검색 + 좋아요순")
    void t22() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 글1", "내용 A"));
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 내용"));

        for (int i = 0; i < 10; i++) p1.increaseLikeCount();
        for (int i = 0; i < 5; i++) p2.increaseLikeCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE_OR_CONTENT")
                        .param("sort", "LIKES")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 글1")); // p1
    }

    @Test
    @DisplayName("제목+내용 검색 + 조회수순")
    void t23() throws Exception {

        Post p1 = postRepository.save(new Post(member, category, "스프링 글1", "내용 A"));
        Post p2 = postRepository.save(new Post(member, category, "글2", "스프링 내용"));
        Post p3 = postRepository.save(new Post(member, category, "글3", "내용 스프링"));

        for (int i = 0; i < 5; i++) p1.increaseViewCount();
        for (int i = 0; i < 10; i++) p2.increaseViewCount();
        for (int i = 0; i < 5; i++) p3.increaseViewCount();

        mvc.perform(get("/api/posts")
                        .param("keyword", "스프링")
                        .param("searchType", "TITLE_OR_CONTENT")
                        .param("sort", "VIEWS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("글2")); // p2
    }

}