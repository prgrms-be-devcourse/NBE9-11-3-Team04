package com.back.devc.domain.post.post.integration;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostControllerIntegrationTest{

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

    @Autowired
    private JwtProvider jwtProvider;

    private Member member;
    private Category category;

    // =========================
    // SETUP
    // =========================
    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());

        member = memberRepository.save(
                Member.createLocalMember(
                        "post-controller-" + unique + "@test.com",
                        "password123!",
                        "postControllerUser" + unique
                )
        );

        category = new Category("테스트 자유 " + unique);
        category = categoryRepository.save(category);
    }

    private String getAccessToken() {
        return jwtProvider.createAccessToken(member);
    }

    // =========================
    // DETAIL
    // =========================
    @Test
    @DisplayName("게시글 상세 조회")
    void getPostDetail() throws Exception {

        Post post = postRepository.save(
                Post.create(member, category, "테스트3", "테스트3내용")
        );

        mvc.perform(get("/api/posts/" + post.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("테스트3"));
    }
    // =========================
    // 최신순, 좋아요순, 조회수순
    // =========================


    @Test
    @DisplayName("게시글 최신순 조회")
    void getPostsByLatest() throws Exception {

        Post post1 = postRepository.save(
                Post.create(member, category, "첫번째", "내용1")
        );

        Thread.sleep(10);

        Post post2 = postRepository.save(
                Post.create(member, category, "두번째", "내용2")
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
    void getPostsByLikesOrder_whenSameLikesThenLatest() throws Exception {

        // given
        Post post1 = postRepository.save(Post.create(member, category, "제목1", "내용1"));
        Thread.sleep(10); // createdAt 차이
        Post post2 = postRepository.save(Post.create(member, category, "제목2", "내용2"));
        Thread.sleep(10);
        Post post3 = postRepository.save(Post.create(member, category, "제목3", "내용3"));

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
    void getPostsByViewsOrder_whenSameViewsThenLatest() throws Exception {

        // given
        Post post1 = postRepository.save(Post.create(member, category, "제목1", "내용1"));
        Thread.sleep(10);
        Post post2 = postRepository.save(Post.create(member, category, "제목2", "내용2"));
        Thread.sleep(10);
        Post post3 = postRepository.save(Post.create(member, category, "제목3", "내용3"));

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
    void getPostsByCategory() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("테스트 공지"));

        // category1 게시글
        postRepository.save(Post.create(member, category, "자유1", "내용1"));
        postRepository.save(Post.create(member, category, "자유2", "내용2"));

        // category2 게시글
        postRepository.save(Post.create(member, category2, "공지1", "내용3"));

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
    void getPostsByCategoryAndLatest() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(Post.create(member, category, "자유1", "내용1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(Post.create(member, category, "자유2", "내용2"));

        // 다른 카테고리
        postRepository.save(Post.create(member, category2, "공지1", "내용3"));

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
    void getPostsByCategoryAndLikes() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(Post.create(member, category, "자유1", "내용1"));
        Post p2 = postRepository.save(Post.create(member, category, "자유2", "내용2"));

        // 좋아요 차이
        for (int i = 0; i < 5; i++) p1.increaseLikeCount();
        for (int i = 0; i < 10; i++) p2.increaseLikeCount();

        // 다른 카테고리
        Post other = postRepository.save(Post.create(member, category2, "공지1", "내용3"));
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
    void getPostsByCategoryAndViews() throws Exception {

        // given
        Category category2 = categoryRepository.save(new Category("공지"));

        Post p1 = postRepository.save(Post.create(member, category, "자유1", "내용1"));
        Post p2 = postRepository.save(Post.create(member, category, "자유2", "내용2"));

        // 조회수 차이
        for (int i = 0; i < 5; i++) p1.increaseViewCount();
        for (int i = 0; i < 10; i++) p2.increaseViewCount();

        // 다른 카테고리
        Post other = postRepository.save(Post.create(member, category2, "공지1", "내용3"));
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
    void searchPostsByTitle() throws Exception {

        postRepository.save(Post.create(member, category, "스프링 공부", "내용1"));
        postRepository.save(Post.create(member, category, "자바 공부", "내용2"));
        postRepository.save(Post.create(member, category, "리액트 공부", "내용3"));

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
    void searchPostsByContent() throws Exception {

        postRepository.save(Post.create(member, category, "글1", "스프링부트 강의"));
        postRepository.save(Post.create(member, category, "글2", "자바 강의"));
        postRepository.save(Post.create(member, category, "글3", "스프링 핵심"));

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
    void searchPostsByTitleOrContent() throws Exception {

        postRepository.save(Post.create(member, category, "스프링", "자바 내용"));
        postRepository.save(Post.create(member, category, "자바", "스프링 내용"));
        postRepository.save(Post.create(member, category, "리액트", "프론트"));

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
    void searchPostsByTitleAndLatest() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 1", "내용1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(Post.create(member, category, "스프링 2", "내용2"));

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
    void searchPostsByTitleAndLikes() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 1", "내용1"));
        Post p2 = postRepository.save(Post.create(member, category, "스프링 2", "내용2"));

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
    void searchPostsByTitleAndViews() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 1", "내용1"));
        Post p2 = postRepository.save(Post.create(member, category, "스프링 2", "내용2"));
        Post p3 = postRepository.save(Post.create(member, category, "스프링 3", "내용3"));

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
    void searchPostsByContentAndLatest() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "글1", "스프링 1"));
        Thread.sleep(10);
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 2"));

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
    void searchPostsByContentAndLikes() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "글1", "스프링 1"));
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 2"));

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
    void searchPostsByContentAndViews() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "글1", "스프링 1"));
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 2"));
        Post p3 = postRepository.save(Post.create(member, category, "글3", "스프링 3"));

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
    void searchPostsByTitleOrContentAndLatest() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"));
        Thread.sleep(10);
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 내용"));

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
    void searchPostsByTitleOrContentAndLikes() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"));
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 내용"));

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
    void searchPostsByTitleOrContentAndViews() throws Exception {

        Post p1 = postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"));
        Post p2 = postRepository.save(Post.create(member, category, "글2", "스프링 내용"));
        Post p3 = postRepository.save(Post.create(member, category, "글3", "내용 스프링"));

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