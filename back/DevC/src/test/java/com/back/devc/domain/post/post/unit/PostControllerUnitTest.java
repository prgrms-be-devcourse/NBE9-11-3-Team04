package com.back.devc.domain.post.post.unit;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.dto.PostCreateRequest;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class PostControllerUnitTest{

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

    @Test
    @DisplayName("게시글 생성")
    void createPost_Success() throws Exception {

        PostCreateRequest request = new PostCreateRequest(
                "테스트글",
                "테스트내용입니다.",
                category.getCategoryId()
        );

        mvc.perform(
                        post("/api/posts")
                                .header("Authorization", "Bearer " + getAccessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").exists())
                .andExpect(jsonPath("$.message").value("게시글 작성 성공"));
    }

    @Test
    @DisplayName("게시글 수정")
    void updatePost_Success() throws Exception {

        Post post = postRepository.save(
                new Post(member, category, "수정 전 제목", "수정 전 내용")
        );

        mvc.perform(
                        put("/api/posts/" + post.getPostId())
                                .header("Authorization", "Bearer " + getAccessToken())
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

    @Test
    @DisplayName("게시글 삭제")
    void deletePost_Success() throws Exception {

        Post post = postRepository.save(
                new Post(member, category, "title", "content")
        );

        mvc.perform(delete("/api/posts/" + post.getPostId())
                        .header("Authorization", "Bearer " + getAccessToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 삭제 성공"));

        Post deleted = postRepository.findById(post.getPostId()).orElseThrow();

        assertThat(deleted.getIsDeleted()).isTrue();
    }

}