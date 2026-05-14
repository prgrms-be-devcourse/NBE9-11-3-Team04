package com.back.devc.domain.interaction.bookmark.entity;

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
        name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookmark_user_post", columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Bookmark(Member member, Post post, LocalDateTime createdAt) {
        this.member = member;
        this.post = post;
        this.createdAt = createdAt;
    }

    public static Bookmark create(Member member, Post post) {
        return Bookmark.builder()
                .member(member)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();
    }
}