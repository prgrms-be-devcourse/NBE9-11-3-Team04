package com.back.devc.domain.interaction.report.entity

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.ReportErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.LocalDateTime

@Entity
@Table(
    name = "report_groups",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_report_groups_target",
            columnNames = ["target_type", "target_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_report_groups_status_latest",
            columnList = "status, latest_reported_at"
        ),
        Index(
            name = "idx_report_groups_target",
            columnList = "target_type, target_id"
        )
    ]
)
class ReportGroup(
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    val targetType: TargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    firstReportedAt: LocalDateTime
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_group_id")
    var reportGroupId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ReportGroupStatus = ReportGroupStatus.OPEN
        protected set

    @Column(name = "report_count", nullable = false)
    var reportCount: Long = 0
        protected set

    @Column(name = "latest_reported_at", nullable = false)
    var latestReportedAt: LocalDateTime = firstReportedAt
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processedByAdmin: Member? = null
        protected set

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
        protected set

    @Column(name = "admin_note", columnDefinition = "TEXT")
    var adminNote: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", length = 30)
    var sanctionType: SanctionType? = null
        protected set

    @Column(name = "suspension_days")
    var suspensionDays: Int? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    fun registerReport(reportedAt: LocalDateTime) {
        if (status != ReportGroupStatus.OPEN) {
            throw ApiException(ReportErrorCode.REPORT_GROUP_409_ALREADY_REPORT)
        }
        reportCount += 1

        if (reportedAt.isAfter(latestReportedAt)) {
            latestReportedAt = reportedAt
        }
    }

    fun approve(
        admin: Member,
        note: String?,
        sanctionType: SanctionType?,
        suspensionDays: Int?,
        now: LocalDateTime
    ) {
        validateOpen()

        status = ReportGroupStatus.APPROVED
        processedByAdmin = admin
        processedAt = now
        adminNote = note
        this.sanctionType = sanctionType
        this.suspensionDays = suspensionDays
    }

    fun reject(
        admin: Member,
        note: String?,
        now: LocalDateTime
    ) {
        validateOpen()

        status = ReportGroupStatus.REJECTED
        processedByAdmin = admin
        processedAt = now
        adminNote = note
        sanctionType = null
        suspensionDays = null
    }

    private fun validateOpen() {
        if (status != ReportGroupStatus.OPEN) {
            throw ApiException(ReportErrorCode.REPORT_GROUP_409_ALREADY_REPORT)
        }
    }
}
