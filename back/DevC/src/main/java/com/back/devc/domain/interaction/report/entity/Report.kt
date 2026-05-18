package com.back.devc.domain.interaction.report.entity

import com.back.devc.domain.member.member.entity.Member
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "reports",
    uniqueConstraints = [
        UniqueConstraint(
            name = "unique_report_per_target",
            columnNames = ["reporter_user_id", "target_type", "target_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_reports_status_created_target",
            columnList = "status, created_at, target_type, target_id"
        ),
        Index(
            name = "idx_reports_status_target_created",
            columnList = "status, target_type, target_id, created_at"
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Report(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    var reporter: Member,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    var targetType: TargetType,

    @Column(name = "target_id", nullable = false)
    var targetId: Long,

    @Column(name = "reason_type", nullable = false, length = 50)
    var reasonType: String,

    @Column(name = "reason_detail", columnDefinition = "TEXT")
    var reasonDetail: String? = null

) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    var reportId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.PENDING
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processedByAdmin: Member? = null
        protected set

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
        protected set

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    protected constructor() : this(
        reporter = null!!,
        targetType = TargetType.POST,
        targetId = 0L,
        reasonType = ""
    )

    fun processReport(admin: Member) {
        processedByAdmin = admin
        status = ReportStatus.RESOLVED
        processedAt = LocalDateTime.now()
    }

    fun rejectReport(admin: Member) {
        processedByAdmin = admin
        status = ReportStatus.REJECTED
        processedAt = LocalDateTime.now()
    }

    companion object {
        @JvmStatic
        fun create(
            reporter: Member,
            targetType: TargetType,
            targetId: Long,
            reasonType: String,
            reasonDetail: String? = null
        ): Report {
            return Report(
                reporter = reporter,
                targetType = targetType,
                targetId = targetId,
                reasonType = reasonType,
                reasonDetail = reasonDetail
            )
        }
    }
}

