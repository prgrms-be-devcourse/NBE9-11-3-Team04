package com.back.devc.domain.post.post.unit;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AdminPostControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private Member member;
    private Member adminMember;
    private Category category;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(
                Member.createLocalMember("test@test.com", "password123!", "testUser")
        );

        adminMember = memberRepository.save(
                Member.createLocalAdminMember("admin@test.com", "password123!", "adminUser")
        );

        category = categoryRepository.save(new Category("테스트 자유"));
    }

    private String getUserToken() {
        return jwtProvider.createAccessToken(member);
    }

    private String getAdminToken() {
        return jwtProvider.createAccessToken(adminMember);
    }

    @Test
    @DisplayName("관리자가 아니면 관리자 API 접근 불가")
    void t1() throws Exception {
        mvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer " + getUserToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 전체 조회 - 관리자용 (삭제 포함, 개수 유지)")
    void t2() throws Exception {
        postRepository.save(new Post(member, category, "글1", "내용1"));
        Post post2 = postRepository.save(new Post(member, category, "글2", "내용2"));
        postRepository.save(new Post(member, category, "글3", "내용3"));

        post2.delete();
        postRepository.saveAndFlush(post2);

        mvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].isDeleted").value(true));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 관리자용 (삭제된 게시글도 조회 가능)")
    void t3() throws Exception {
        Post deletedPost = postRepository.save(new Post(member, category, "삭제된 글", "내용"));

        deletedPost.delete();
        postRepository.saveAndFlush(deletedPost);

        mvc.perform(get("/api/admin/posts/{postId}", deletedPost.getPostId())
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("삭제된 글"))
                .andExpect(jsonPath("$.isDeleted").value(true));
    }
}