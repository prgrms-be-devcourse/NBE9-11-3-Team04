package com.back.devc.domain.member.searchLog.entity;

import com.back.devc.domain.member.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_log_id")
    private Long searchLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    public SearchLog(Member member, String keyword) {
        this.member = member;
        this.keyword = keyword;
        this.searchedAt = LocalDateTime.now();
    }
}