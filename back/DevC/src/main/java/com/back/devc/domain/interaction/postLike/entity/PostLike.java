package com.back.devc.domain.interaction.postLike.entity;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.post.post.entity.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_like_user_post", columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 외부에서 builder를 직접 열지 않고,
     * create()를 통해서만 생성 규칙을 타도록 제한한다.
     */
    @Builder(access = AccessLevel.PRIVATE)
    private PostLike(Member member, Post post, LocalDateTime createdAt) {
        this.member = member;
        this.post = post;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * 좋아요 엔티티 생성 정적 팩토리 메서드
     */
    public static PostLike create(Member member, Post post) {
        return PostLike.builder()
                .member(member)
                .post(post)
                .build();
    }
}