package com.back.devc.domain.interaction.report.entity

import com.back.devc.domain.member.member.entity.Member
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
import java.time.LocalDateTime

@Entity
@Table(
    name = "report_group_actions",
    indexes = [
        Index(
            name = "idx_report_group_actions_group_created",
            columnList = "report_group_id, created_at"
        ),
        Index(
            name = "idx_report_group_actions_admin",
            columnList = "admin_user_id"
        )
    ]
)
class ReportGroupAction(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_group_id", nullable = false)
    val reportGroup: ReportGroup,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    val admin: Member,

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    val actionType: ReportGroupActionType,

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", nullable = false, length = 30)
    val beforeStatus: ReportGroupStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", nullable = false, length = 30)
    val afterStatus: ReportGroupStatus,

    @Column(name = "note", columnDefinition = "TEXT")
    val note: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", length = 30)
    val sanctionType: SanctionType?,

    @Column(name = "suspension_days")
    val suspensionDays: Int?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_group_action_id")
    var reportGroupActionId: Long? = null
        protected set

    companion object {
        fun approve(
            reportGroup: ReportGroup,
            admin: Member,
            beforeStatus: ReportGroupStatus,
            note: String?,
            sanctionType: SanctionType?,
            suspensionDays: Int?,
            now: LocalDateTime
        ): ReportGroupAction {
            return ReportGroupAction(
                reportGroup = reportGroup,
                admin = admin,
                actionType = ReportGroupActionType.APPROVE,
                beforeStatus = beforeStatus,
                afterStatus = ReportGroupStatus.APPROVED,
                note = note,
                sanctionType = sanctionType,
                suspensionDays = suspensionDays,
                createdAt = now
            )
        }

        fun reject(
            reportGroup: ReportGroup,
            admin: Member,
            beforeStatus: ReportGroupStatus,
            note: String?,
            now: LocalDateTime
        ): ReportGroupAction {
            return ReportGroupAction(
                reportGroup = reportGroup,
                admin = admin,
                actionType = ReportGroupActionType.REJECT,
                beforeStatus = beforeStatus,
                afterStatus = ReportGroupStatus.REJECTED,
                note = note,
                sanctionType = null,
                suspensionDays = null,
                createdAt = now
            )
        }
    }
}
