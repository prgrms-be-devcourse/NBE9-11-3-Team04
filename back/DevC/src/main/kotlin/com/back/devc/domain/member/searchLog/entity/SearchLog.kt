package com.back.devc.domain.member.searchLog.entity

import com.back.devc.domain.member.member.entity.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "search_logs")
class SearchLog protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_log_id")
    var searchLogId: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    lateinit var member: Member
        protected set

    @Column(name = "keyword", nullable = false, length = 255)
    lateinit var keyword: String
        protected set

    @Column(name = "searched_at", nullable = false)
    lateinit var searchedAt: LocalDateTime
        protected set

    constructor(
        member: Member,
        keyword: String,
    ) : this() {
        this.member = member
        this.keyword = keyword
        this.searchedAt = LocalDateTime.now()
    }
}