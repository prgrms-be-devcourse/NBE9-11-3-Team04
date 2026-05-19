package com.back.devc.domain.post.post.integration

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.BeforeEach
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
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class PostControllerIntegrationTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    private lateinit var member: Member
    private lateinit var category: Category

    private val accessToken: String
        get() = jwtProvider.createAccessToken(member)

    @BeforeEach
    fun setUp() {
        val unique = System.nanoTime().toString()

        member = memberRepository.save(
            Member.createLocalMember(
                email = "post-controller-$unique@test.com",
                passwordHash = "password123!",
                nickname = "postControllerUser$unique",
            ),
        )

        category = categoryRepository.save(Category("테스트 자유 $unique"))
    }

    @Test
    @DisplayName("게시글 상세 조회")
    fun getPostDetail() {
        val post = postRepository.save(
            Post.create(member, category, "테스트3", "테스트3내용"),
        )

        mvc.perform(MockMvcRequestBuilders.get("/api/posts/{postId}", post.postId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("테스트3"))
    }

    @Test
    @DisplayName("게시글 최신순 조회")
    fun getPostsByLatest() {
        postRepository.save(Post.create(member, category, "첫번째", "내용1"))

        Thread.sleep(10)

        postRepository.save(Post.create(member, category, "두번째", "내용2"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("sort", "LATEST")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("두번째"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("첫번째"))
    }

    @Test
    @DisplayName("게시글 좋아요순 조회, 만약 좋아요 개수가 같은경우 최신순으로 보여줌")
    fun getPostsByLikesOrderWhenSameLikesThenLatest() {
        val post1 = postRepository.save(Post.create(member, category, "제목1", "내용1"))
        Thread.sleep(10)
        val post2 = postRepository.save(Post.create(member, category, "제목2", "내용2"))
        Thread.sleep(10)
        val post3 = postRepository.save(Post.create(member, category, "제목3", "내용3"))

        repeat(5) { post1.increaseLikeCount() }
        repeat(10) { post2.increaseLikeCount() }
        repeat(5) { post3.increaseLikeCount() }

        postRepository.flush()

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10"),
        )

        result.andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("제목2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("제목3"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[2].title").value("제목1"))
    }

    @Test
    @DisplayName("게시글 조회수 순서 조회, 만약 조회수 개수가 같은경우 최신순으로 보여준다")
    fun getPostsByViewsOrderWhenSameViewsThenLatest() {
        val post1 = postRepository.save(Post.create(member, category, "제목1", "내용1"))
        Thread.sleep(10)
        val post2 = postRepository.save(Post.create(member, category, "제목2", "내용2"))
        Thread.sleep(10)
        val post3 = postRepository.save(Post.create(member, category, "제목3", "내용3"))

        repeat(5) { post1.increaseViewCount() }
        repeat(10) { post2.increaseViewCount() }
        repeat(5) { post3.increaseViewCount() }

        postRepository.flush()

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10"),
        )

        result.andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("제목2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("제목3"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[2].title").value("제목1"))
    }

    @Test
    @DisplayName("게시글 카테고리별 조회")
    fun getPostsByCategory() {
        val category2 = categoryRepository.save(Category("테스트 공지"))

        postRepository.save(Post.create(member, category, "자유1", "내용1"))
        postRepository.save(Post.create(member, category, "자유2", "내용2"))
        postRepository.save(Post.create(member, category2, "공지1", "내용3"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("categoryId", category.categoryId.toString())
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].categoryId").value(category.categoryId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].categoryId").value(category.categoryId))
    }

    @Test
    @DisplayName("카테고리 + 최신순 조회")
    fun getPostsByCategoryAndLatest() {
        val category2 = categoryRepository.save(Category("공지"))

        postRepository.save(Post.create(member, category, "자유1", "내용1"))
        Thread.sleep(10)
        postRepository.save(Post.create(member, category, "자유2", "내용2"))
        postRepository.save(Post.create(member, category2, "공지1", "내용3"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("categoryId", category.categoryId.toString())
                .param("sort", "LATEST")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("자유2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("자유1"))
    }

    @Test
    @DisplayName("카테고리 + 좋아요순 조회")
    fun getPostsByCategoryAndLikes() {
        val category2 = categoryRepository.save(Category("공지"))

        val post1 = postRepository.save(Post.create(member, category, "자유1", "내용1"))
        val post2 = postRepository.save(Post.create(member, category, "자유2", "내용2"))

        repeat(5) { post1.increaseLikeCount() }
        repeat(10) { post2.increaseLikeCount() }

        val other = postRepository.save(Post.create(member, category2, "공지1", "내용3"))
        repeat(100) { other.increaseLikeCount() }

        postRepository.flush()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("categoryId", category.categoryId.toString())
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("자유2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("자유1"))
    }

    @Test
    @DisplayName("카테고리 + 조회수순 조회")
    fun getPostsByCategoryAndViews() {
        val category2 = categoryRepository.save(Category("공지"))

        val post1 = postRepository.save(Post.create(member, category, "자유1", "내용1"))
        val post2 = postRepository.save(Post.create(member, category, "자유2", "내용2"))

        repeat(5) { post1.increaseViewCount() }
        repeat(10) { post2.increaseViewCount() }

        val other = postRepository.save(Post.create(member, category2, "공지1", "내용3"))
        repeat(100) { other.increaseViewCount() }

        postRepository.flush()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("categoryId", category.categoryId.toString())
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("자유2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].title").value("자유1"))
    }

    @Test
    @DisplayName("게시글 제목 검색")
    fun searchPostsByTitle() {
        postRepository.save(Post.create(member, category, "스프링 공부", "내용1"))
        postRepository.save(Post.create(member, category, "자바 공부", "내용2"))
        postRepository.save(Post.create(member, category, "리액트 공부", "내용3"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("스프링 공부"))
    }

    @Test
    @DisplayName("게시글 내용 검색")
    fun searchPostsByContent() {
        postRepository.save(Post.create(member, category, "글1", "스프링부트 강의"))
        postRepository.save(Post.create(member, category, "글2", "자바 강의"))
        postRepository.save(Post.create(member, category, "글3", "스프링 핵심"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "CONTENT")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
    }

    @Test
    @DisplayName("게시글 제목 + 내용 검색")
    fun searchPostsByTitleOrContent() {
        postRepository.save(Post.create(member, category, "스프링", "자바 내용"))
        postRepository.save(Post.create(member, category, "자바", "스프링 내용"))
        postRepository.save(Post.create(member, category, "리액트", "프론트"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE_OR_CONTENT")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
    }

    @Test
    @DisplayName("제목 검색 + 최신순")
    fun searchPostsByTitleAndLatest() {
        postRepository.save(Post.create(member, category, "스프링 1", "내용1"))
        Thread.sleep(10)
        postRepository.save(Post.create(member, category, "스프링 2", "내용2"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE")
                .param("sort", "LATEST")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("스프링 2"))
    }

    @Test
    @DisplayName("제목 검색 + 좋아요순")
    fun searchPostsByTitleAndLikes() {
        val post1 = postRepository.save(Post.create(member, category, "스프링 1", "내용1"))
        val post2 = postRepository.save(Post.create(member, category, "스프링 2", "내용2"))

        repeat(10) { post1.increaseLikeCount() }
        repeat(5) { post2.increaseLikeCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE")
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("스프링 1"))
    }

    @Test
    @DisplayName("제목 검색 + 조회수순")
    fun searchPostsByTitleAndViews() {
        val post1 = postRepository.save(Post.create(member, category, "스프링 1", "내용1"))
        val post2 = postRepository.save(Post.create(member, category, "스프링 2", "내용2"))
        val post3 = postRepository.save(Post.create(member, category, "스프링 3", "내용3"))

        repeat(5) { post1.increaseViewCount() }
        repeat(10) { post2.increaseViewCount() }
        repeat(5) { post3.increaseViewCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE")
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("스프링 2"))
    }

    @Test
    @DisplayName("내용 검색 + 최신순")
    fun searchPostsByContentAndLatest() {
        postRepository.save(Post.create(member, category, "글1", "스프링 1"))
        Thread.sleep(10)
        postRepository.save(Post.create(member, category, "글2", "스프링 2"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "CONTENT")
                .param("sort", "LATEST")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("글2"))
    }

    @Test
    @DisplayName("내용 검색 + 좋아요순")
    fun searchPostsByContentAndLikes() {
        val post1 = postRepository.save(Post.create(member, category, "글1", "스프링 1"))
        val post2 = postRepository.save(Post.create(member, category, "글2", "스프링 2"))

        repeat(10) { post1.increaseLikeCount() }
        repeat(5) { post2.increaseLikeCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "CONTENT")
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("글1"))
    }

    @Test
    @DisplayName("내용 검색 + 조회수순")
    fun searchPostsByContentAndViews() {
        val post1 = postRepository.save(Post.create(member, category, "글1", "스프링 1"))
        val post2 = postRepository.save(Post.create(member, category, "글2", "스프링 2"))
        val post3 = postRepository.save(Post.create(member, category, "글3", "스프링 3"))

        repeat(5) { post1.increaseViewCount() }
        repeat(10) { post2.increaseViewCount() }
        repeat(5) { post3.increaseViewCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "CONTENT")
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("글2"))
    }

    @Test
    @DisplayName("제목+내용 검색 + 최신순")
    fun searchPostsByTitleOrContentAndLatest() {
        postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"))
        Thread.sleep(10)
        postRepository.save(Post.create(member, category, "글2", "스프링 내용"))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE_OR_CONTENT")
                .param("sort", "LATEST")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("글2"))
    }

    @Test
    @DisplayName("제목+내용 검색 + 좋아요순")
    fun searchPostsByTitleOrContentAndLikes() {
        val post1 = postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"))
        val post2 = postRepository.save(Post.create(member, category, "글2", "스프링 내용"))

        repeat(10) { post1.increaseLikeCount() }
        repeat(5) { post2.increaseLikeCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE_OR_CONTENT")
                .param("sort", "LIKES")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("스프링 글1"))
    }

    @Test
    @DisplayName("제목+내용 검색 + 조회수순")
    fun searchPostsByTitleOrContentAndViews() {
        val post1 = postRepository.save(Post.create(member, category, "스프링 글1", "내용 A"))
        val post2 = postRepository.save(Post.create(member, category, "글2", "스프링 내용"))
        val post3 = postRepository.save(Post.create(member, category, "글3", "내용 스프링"))

        repeat(5) { post1.increaseViewCount() }
        repeat(10) { post2.increaseViewCount() }
        repeat(5) { post3.increaseViewCount() }

        mvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
                .param("keyword", "스프링")
                .param("searchType", "TITLE_OR_CONTENT")
                .param("sort", "VIEWS")
                .param("page", "0")
                .param("size", "10"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("글2"))
    }
}