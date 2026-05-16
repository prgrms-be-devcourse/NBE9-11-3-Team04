package com.back.devc.domain.member.member.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_users_nickname", columnNames = ["nickname"]),
        UniqueConstraint(
            name = "uk_users_provider_provider_user_id",
            columnNames = ["provider", "provider_user_id"]
        )
    ]
)
class Member protected constructor() {

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "user_id")
    var userId: Long? = null
        protected set

    @field:Column(name = "email", nullable = false, length = 255)
    lateinit var email: String
        protected set

    @field:Column(name = "password_hash", nullable = false, length = 255)
    lateinit var passwordHash: String
        protected set

    @field:Column(name = "nickname", nullable = false, length = 50)
    lateinit var nickname: String
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "role", nullable = false, length = 20)
    lateinit var role: MemberRole
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "status", nullable = false, length = 20)
    lateinit var status: MemberStatus
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "provider", nullable = false, length = 20)
    lateinit var provider: AuthProvider
        protected set

    @field:Column(name = "provider_user_id", nullable = false, length = 100)
    lateinit var providerUserId: String
        protected set

    @field:Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @field:Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @field:Column(nullable = true)
    var suspendedUntil: LocalDateTime? = null

    private constructor(
        email: String,
        passwordHash: String,
        nickname: String,
        role: MemberRole,
        status: MemberStatus,
        provider: AuthProvider,
        providerUserId: String
    ) : this() {
        this.email = email
        this.passwordHash = passwordHash
        this.nickname = nickname
        this.role = role
        this.status = status
        this.provider = provider
        this.providerUserId = providerUserId
    }

    companion object {
        @JvmStatic
        fun createLocalMember(
            email: String,
            passwordHash: String,
            nickname: String
        ): Member {
            return Member(
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                role = MemberRole.USER,
                status = MemberStatus.ACTIVE,
                provider = AuthProvider.LOCAL,
                providerUserId = email
            )
        }

        @JvmStatic
        fun createLocalAdminMember(
            email: String,
            passwordHash: String,
            nickname: String
        ): Member {
            return Member(
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                role = MemberRole.ADMIN,
                status = MemberStatus.ACTIVE,
                provider = AuthProvider.LOCAL,
                providerUserId = email
            )
        }

        @JvmStatic
        fun createOAuthMember(
            provider: AuthProvider,
            providerUserId: String,
            email: String,
            passwordHash: String,
            nickname: String
        ): Member {
            return Member(
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                role = MemberRole.USER,
                status = MemberStatus.ACTIVE,
                provider = provider,
                providerUserId = providerUserId
            )
        }
    }

    fun updateNickname(nickname: String) {
        this.nickname = nickname
    }

    fun updatePasswordHash(passwordHash: String) {
        this.passwordHash = passwordHash
    }

    fun updateRole(role: MemberRole) {
        this.role = role
    }

    fun updateStatus(newStatus: MemberStatus?) {
        if (newStatus == null) {
            throw IllegalArgumentException("변경할 상태값이 비어있습니다.")
        }
        status = newStatus
    }

    fun withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            throw IllegalStateException("이미 탈퇴한 회원입니다.")
        }

        status = MemberStatus.WITHDRAWN
        email = "withdrawn_$userId@deleted.local"
        nickname = "withdrawn_$userId"
        passwordHash = "WITHDRAWN_USER"
        providerUserId = "withdrawn_$userId"
    }

    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()

        if (!::role.isInitialized) {
            role = MemberRole.USER
        }

        if (!::status.isInitialized) {
            status = MemberStatus.ACTIVE
        }

        if (!::provider.isInitialized) {
            provider = AuthProvider.LOCAL
        }

        if (!::providerUserId.isInitialized || providerUserId.isBlank()) {
            providerUserId = email
        }

        createdAt = now
        updatedAt = now
    }

    fun isAdmin(): Boolean {
        return role == MemberRole.ADMIN
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}