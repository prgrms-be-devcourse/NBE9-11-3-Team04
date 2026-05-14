package com.back.devc.domain.post.post.controller;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;



@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
public class AdminPostControllerTest {
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


    @Test
    @DisplayName("관리자가 아니면 관리자 API 접근 불가")
    @WithMockUser(roles = "USER") // 유저 권한인 경우에 -> block 되는 것을 보여주는 테스트
    void t1() throws Exception {

        mvc.perform(get("/api/admin/posts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 전체 조회 - 관리자용 (삭제 포함, 개수 유지)")
    void t2() throws Exception {

        Post post1 = postRepository.save(
                new Post(member, category, "글1", "내용1")
        );
        Post post2 = postRepository.save(
                new Post(member, category, "글2", "내용2")
        );
        Post post3 = postRepository.save(
                new Post(member, category, "글3", "내용3")
        );

        // soft delete
        post2.delete();
        postRepository.flush();

        mvc.perform(get("/api/admin/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].isDeleted").value(true));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 관리자용 (삭제된 게시글도 조회 가능)")
    void t3() throws Exception {

        // given
        Post deletedPost = postRepository.save(
                new Post(member, category, "삭제된 글", "내용")
        );

        deletedPost.delete(); // soft delete
        postRepository.flush();

        // when & then
        mvc.perform(get("/api/admin/posts/{postId}", deletedPost.getPostId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("삭제된 글"))
                .andExpect(jsonPath("$.isDeleted").value(true));
    }
    
}
