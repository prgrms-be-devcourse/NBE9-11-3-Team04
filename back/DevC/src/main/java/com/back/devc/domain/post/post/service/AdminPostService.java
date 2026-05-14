package com.back.devc.domain.post.post.service;

import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.dto.AdminPostDetailResponse;
import com.back.devc.domain.post.post.dto.PostDetailResponse;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final PostRepository postRepository;

    // 전체 조회 (최신순 - 관리자용 isDeleted=true 포함)
    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }


    @Transactional(readOnly = true)
    public AdminPostDetailResponse findDetailById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. id=" + postId));

        return AdminPostDetailResponse.from(post);
    }
}
