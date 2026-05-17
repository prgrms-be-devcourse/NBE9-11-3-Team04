package com.back.devc.domain.interaction.bookmark.entity

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.post.entity.Post
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bookmark_user_post",
            columnNames = ["user_id", "post_id"],
        ),
    ],
)
class Bookmark protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    var bookmarkId: Long? = null
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
        @JvmStatic
        fun create(
            member: Member,
            post: Post,
        ): Bookmark {
            return Bookmark(
                member = member,
                post = post,
                createdAt = LocalDateTime.now(),
            )
        }
    }
}