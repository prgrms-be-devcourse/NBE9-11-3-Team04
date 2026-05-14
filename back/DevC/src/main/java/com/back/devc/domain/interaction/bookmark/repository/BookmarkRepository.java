package com.back.devc.domain.interaction.bookmark.repository;

import com.back.devc.domain.interaction.bookmark.entity.Bookmark;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberAndPost(Member member, Post post);

    Optional<Bookmark> findByMemberAndPost(Member member, Post post);

    List<Bookmark> findAllByMember(Member member);

    List<Bookmark> findAllByMemberAndPost_IsDeletedFalse(Member member);

    void deleteByPost_PostId(Long postId);

    /**
     * 현재 로그인한 사용자가 특정 게시글을 북마크했는지 확인
     *
     * 게시글 상세조회 응답에서 bookmarked 값을 내려주기 위해 사용
     */
    boolean existsByMember_UserIdAndPost_PostId(Long userId, Long postId);

    /**
     * 특정 게시글의 전체 북마크 수를 조회
     *
     * 게시글 상세 페이지에서 북마크 수를 바로 표시할 수 있도록
     * 상세조회 응답에 bookmarkCount 를 포함할 때 사용
     */
    long countByPost_PostId(Long postId);
}