package com.back.devc.domain.member.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uk_users_provider_provider_user_id", columnNames = {"provider", "provider_user_id"})
        }
)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime suspendedUntil;

    @Builder(access = AccessLevel.PRIVATE)
    private Member(
            String email,
            String passwordHash,
            String nickname,
            MemberRole role,
            MemberStatus status,
            AuthProvider provider,
            String providerUserId
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static Member createLocalMember(String email, String passwordHash, String nickname) {
        return Member.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .providerUserId(email)
                .build();
    }

    public static Member createLocalAdminMember(String email, String passwordHash, String nickname) {
        return Member.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .providerUserId(email)
                .build();
    }

    public static Member createOAuthMember(
            AuthProvider provider,
            String providerUserId,
            String email,
            String passwordHash,
            String nickname
    ) {
        return Member.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateRole(MemberRole role) {
        this.role = role;
    }

    public void updateStatus(MemberStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 상태값이 비어있습니다.");
        }
        this.status = newStatus;
    }

    public void withdraw() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        this.status = MemberStatus.WITHDRAWN;
        this.email = "withdrawn_" + this.userId + "@deleted.local";
        this.nickname = "withdrawn_" + this.userId;
        this.passwordHash = "WITHDRAWN_USER";
        this.providerUserId = "withdrawn_" + this.userId;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (role == null) {
            role = MemberRole.USER;
        }

        if (status == null) {
            status = MemberStatus.ACTIVE;
        }

        if (provider == null) {
            provider = AuthProvider.LOCAL;
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            providerUserId = email;
        }

        createdAt = now;
        updatedAt = now;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }


}