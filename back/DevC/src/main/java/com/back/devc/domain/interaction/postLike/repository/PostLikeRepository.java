package com.back.devc.domain.interaction.postLike.repository;

import com.back.devc.domain.interaction.postLike.entity.PostLike;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 회원이 특정 게시글에 좋아요를 눌렀는지 확인
     */
    boolean existsByMemberAndPost(Member member, Post post);

    /**
     * 특정 회원의 특정 게시글 좋아요 엔티티 조회
     */
    Optional<PostLike> findByMemberAndPost(Member member, Post post);

    /**
     * 특정 회원이 좋아요한 게시글 목록 조회
     */
    List<PostLike> findAllByMember(Member member);

    /**
     * 삭제되지 않은 게시글에 대한 좋아요 목록만 조회
     */
    List<PostLike> findAllByMemberAndPost_IsDeletedFalse(Member member);

    /**
     * 특정 게시글에 연결된 좋아요 전체 삭제
     */
    void deleteByPost_PostId(Long postId);

    /**
     * userId, postId 기반 존재 여부 확인
     * 필요 시 엔티티 조회 없이 빠르게 체크할 수 있다.
     */
    boolean existsByMember_UserIdAndPost_PostId(Long userId, Long postId);
}