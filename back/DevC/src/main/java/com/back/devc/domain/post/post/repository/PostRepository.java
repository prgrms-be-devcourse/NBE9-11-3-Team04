package com.back.devc.domain.post.post.repository;

import com.back.devc.domain.member.member.dto.CountResultDto;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByCategoryCategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    Page<Post> findByTitleContainingAndIsDeletedFalse(String title, Pageable pageable);

    Page<Post> findByContentContainingAndIsDeletedFalse(String content, Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.isDeleted = false
        AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)
        """)
    Page<Post> searchByKeyword(
            @Param("kw") String kw,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Post p
        WHERE p.isDeleted = false
        AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
        AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)
        """)
    Page<Post> searchPosts(
            @Param("categoryId") Long categoryId,
            @Param("kw") String kw,
            Pageable pageable
    );

    Page<Post> findByCategoryCategoryIdAndTitleContainingAndIsDeletedFalse(
            Long categoryId, String title, Pageable pageable
    );

    Page<Post> findByCategoryCategoryIdAndContentContainingAndIsDeletedFalse(
            Long categoryId, String content, Pageable pageable
    );

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByMember(Member member);

    List<Post> findAllByMemberAndIsDeletedFalse(Member member);

    List<Post> findTop20ByMemberAndIsDeletedFalseOrderByCreatedAtDesc(Member member);

    Optional<Post> findByPostIdAndIsDeletedFalse(Long id);

    List<Post> findByIsDeletedFalse();

    List<Post> findByIsDeletedFalseOrderByCreatedAtDesc();

    List<Post> findByIsDeletedFalseOrderByViewCountDesc();

    List<Post> findByIsDeletedFalseOrderByLikeCountDesc();

    List<Post> findByCategoryCategoryIdAndIsDeletedFalse(long categoryId);

    long countByMember(Member member);

    // Batch IN 신고 조회용
    List<Post> findAllByPostIdIn(List<Long> postIds);

    // Batch IN 유저 목록 조회용
    @Query("""
        SELECT new com.back.devc.domain.member.member.dto.CountResultDto(p.member.userId, COUNT(p))
        FROM Post p
        WHERE p.member.userId IN :userIds
        GROUP BY p.member.userId
    """)
    List<CountResultDto> countPostsByUserIds(@Param("userIds") List<Long> userIds);
}