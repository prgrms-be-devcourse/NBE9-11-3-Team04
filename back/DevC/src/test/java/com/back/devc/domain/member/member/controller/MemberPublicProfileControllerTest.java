package com.back.devc.domain.member.member.controller;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MemberPublicProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("공개 프로필 조회 - 정상 응답")
    void getPublicProfile_success() throws Exception {
        Member member = memberRepository.save(
                Member.createLocalMember("public-profile@test.com", "dummy-password", "publicUser")
        );
        Category category = categoryRepository.save(new Category("public-profile-category"));

        Post visiblePost1 = postRepository.save(new Post(member, category, "공개 글 1", "내용 1"));
        Post visiblePost2 = postRepository.save(new Post(member, category, "공개 글 2", "내용 2"));
        Post deletedPost = postRepository.save(new Post(member, category, "삭제된 글", "내용 3"));
        deletedPost.delete();
        postRepository.flush();

        mvc.perform(get("/api/users/{userId}/profile", member.getUserId()))
                .andDo(print())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("getPublicProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_PUBLIC_PROFILE_SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(member.getUserId()))
                .andExpect(jsonPath("$.data.nickname").value("publicUser"))
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.posts[?(@.title == '공개 글 1')]").isNotEmpty())
                .andExpect(jsonPath("$.data.posts[?(@.title == '공개 글 2')]").isNotEmpty())
                .andExpect(jsonPath("$.data.posts[?(@.title == '삭제된 글')]").isEmpty());
    }

    @Test
    @DisplayName("공개 프로필 조회 - 존재하지 않는 사용자")
    void getPublicProfile_memberNotFound() throws Exception {
        long notExistsUserId = 999_999L;

        mvc.perform(get("/api/users/{userId}/profile", notExistsUserId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_404_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개 프로필 조회 - 게시글 없는 사용자")
    void getPublicProfile_emptyPosts() throws Exception {
        Member member = memberRepository.save(
                Member.createLocalMember("empty-posts@test.com", "dummy-password", "noPostsUser")
        );

        mvc.perform(get("/api/users/{userId}/profile", member.getUserId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_PUBLIC_PROFILE_SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(member.getUserId()))
                .andExpect(jsonPath("$.data.nickname").value("noPostsUser"))
                .andExpect(jsonPath("$.data.posts.length()").value(0));
    }
}
