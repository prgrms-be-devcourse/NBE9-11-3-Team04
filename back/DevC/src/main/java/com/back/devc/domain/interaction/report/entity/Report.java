package com.back.devc.domain.interaction.report.entity;


import com.back.devc.domain.member.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
/*
* 신고는 동일한 사용자가 동일한 대상(게시글 또는 댓글)에 대해 중복 신고하는 것을 방지하기 위해
* reporter_user_id + target_type + target_id 조합에 대해 유니크 제약조건을 설정
 */
@Table(
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_report_per_target",
                        columnNames = {"reporter_user_id", "target_type", "target_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성일 자동 기록을 위해 필요
@ToString(exclude = {"reporter", "processedByAdmin"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private Member reporter;

    @Enumerated(EnumType.STRING)
    private TargetType  targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType;

    @Column(name = "reason_detail", columnDefinition = "TEXT")
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Member processedByAdmin;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Report(Member reporter,
                  TargetType  targetType,
                  Long targetId,
                  String reasonType,
                  String reasonDetail) {

        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonType = reasonType;
        this.reasonDetail = reasonDetail;

        this.status = ReportStatus.PENDING;
    }

    /**
     * 신고 처리
     */
    public void processReport(Member admin) {
        this.processedByAdmin = admin;
        this.status = ReportStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 신고 반려
     */
    public void rejectReport(Member admin) {
        this.processedByAdmin = admin;
        this.status = ReportStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}