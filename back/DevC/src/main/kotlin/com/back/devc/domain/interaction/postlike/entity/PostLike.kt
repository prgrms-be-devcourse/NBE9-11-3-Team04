package com.back.devc.domain.interaction.postLike.entity

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.post.entity.Post
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_like_user_post",
            columnNames = ["user_id", "post_id"],
        ),
    ],
)
class PostLike protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    var likeId: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var member: Member
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    lateinit var post: Post
        protected set

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    private constructor(
        member: Member,
        post: Post,
        createdAt: LocalDateTime,
    ) : this() {
        this.member = member
        this.post = post
        this.createdAt = createdAt
    }

    companion object {
        /**
         * 좋아요 엔티티 생성 정적 팩토리 메서드
         */
        @JvmStatic
        fun create(
            member: Member,
            post: Post,
        ): PostLike {
            return PostLike(
                member = member,
                post = post,
                createdAt = LocalDateTime.now(),
            )
        }
    }
}