package com.back.devc.domain.interaction.report.service

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupAction
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupActionRepository
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime



@Service
@Transactional
class AdminReportService(
    private val reportRepository: ReportRepository,
    private val reportGroupRepository: ReportGroupRepository,
    private val reportGroupActionRepository: ReportGroupActionRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val reportTargetHandler: ReportTargetHandler
) {

    @Transactional(readOnly = true)
    fun getReports(
        status: ReportStatus?,
        pageable: Pageable
    ): Page<ReportResponseDTO> {

        log.info(
            "관리자 신고 단건 목록 조회 시작 - status={}, page={}, size={}",
            status,
            pageable.pageNumber,
            pageable.pageSize
        )

        val reports = status?.let {
            reportRepository.findAllByStatus(it, pageable)
        } ?: reportRepository.findAll(pageable)

        log.info(
            "관리자 신고 단건 목록 조회 완료 - status={}, totalElements={}, totalPages={}, count={}",
            status,
            reports.totalElements,
            reports.totalPages,
            reports.numberOfElements
        )

        return reports.map(reportTargetHandler::toDtoWithTargetInfo)
    }

    @Transactional(readOnly = true)
    @Deprecated(
        message = "Use getGroupedReports(status, pageable) based on ReportGroup instead."
    )
    // 기존 관리자 화면/성능 테스트 호환을 위해 유지한다. 신규 코드는 ReportGroup 기반 조회를 사용한다.
    fun getGroupedReportsNoBatch(
        status: ReportStatus?,
        pageable: Pageable
    ): Page<ReportGroupResponseDTO> {

        val to = LocalDateTime.now()
        val from = to.minusDays(DEFAULT_GROUP_LOOKBACK_DAYS.toLong())

        log.info(
            "관리자 신고 그룹 목록 조회 시작(no batch) - status={}, page={}, size={}",
            status,
            pageable.pageNumber,
            pageable.pageSize
        )

        val result = reportRepository.findGroupedReports(
            status,
            from,
            to,
            pageable
        )

        log.info(
            "관리자 신고 그룹 목록 조회 완료(no batch) - status={}, totalElements={}, totalPages={}, count={}",
            status,
            result.totalElements,
            result.totalPages,
            result.numberOfElements
        )

        return result.map { row ->

            val groupRow = row.toGroupRow()

            val info = reportTargetHandler.getTargetInfo(
                groupRow.targetType,
                groupRow.targetId
            )

            val reasonTypes = reportRepository.findReasonTypesByTargetId(
                groupRow.targetType,
                groupRow.targetId
            )

            ReportGroupResponseDTO(
                groupRow.targetType,
                groupRow.targetId,
                info.nickname,
                info.title,
                info.content,
                groupRow.reportCount,
                reasonTypes,
                status,
                groupRow.latestCreatedAt
            )
        }
    }

    // 전체 조회
    @Transactional(readOnly = true)
    fun getGroupedReports(
        status: ReportStatus?,
        pageable: Pageable
    ): Page<ReportGroupResponseDTO> {

        val to = LocalDateTime.now()
        val from = to.minusDays(DEFAULT_GROUP_LOOKBACK_DAYS.toLong())

        return getGroupedReports(
            status,
            from,
            to,
            pageable
        )
    }

    // 기간 검색 조회
    @Transactional(readOnly = true)
    fun getGroupedReports(
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<ReportGroupResponseDTO> {

        validateGroupedReportSearch(
            from,
            to,
            pageable
        )

        val reportGroupStatus = status.toReportGroupStatus()
        val reportGroupPageable = toReportGroupPageable(pageable)

        log.info(
            "관리자 신고 그룹 목록 조회 시작 - status={}, page={}, size={}",
            status,
            pageable.pageNumber,
            pageable.pageSize
        )

        val result = reportGroupRepository.findReportGroups(
            reportGroupStatus,
            from,
            to,
            reportGroupPageable
        )

        val reportGroups = result.content

        log.debug(
            "관리자 신고 그룹 조회 row 수 - status={}, rowCount={}",
            status,
            reportGroups.size
        )


        val postIds = reportGroups
            .filter { it.targetType == TargetType.POST }
            .map { it.targetId }

        val commentIds = reportGroups
            .filter { it.targetType == TargetType.COMMENT }
            .map { it.targetId }

        log.debug(
            "관리자 신고 그룹 대상 ID 분리 완료 - status={}, postCount={}, commentCount={}",
            status,
            postIds.size,
            commentIds.size
        )

        val postMap: Map<Long, Post> =
            if (postIds.isEmpty()) {
                emptyMap()
            } else {
                postRepository.findAllByPostIdIn(postIds)
                    .associateBy { it.requiredPostId }
            }

        val commentMap: Map<Long, Comment> =
            if (commentIds.isEmpty()) {
                emptyMap()
            } else {
                commentRepository.findAllByIdIn(commentIds)
                    .associateBy { it.requiredCommentId }
            }

        val commentWriterIds = commentMap.values
            .map { it.writerId }
            .distinct()

        val memberMap: Map<Long, Member> =
            if (commentWriterIds.isEmpty()) {
                emptyMap()
            } else {
                memberRepository.findAllById(commentWriterIds)
                    .associateBy { it.requiredUserId }
            }

        val reportGroupIds = reportGroups
            .mapNotNull { it.reportGroupId }

        val reasonTypeMap = loadReasonTypesByReportGroupIds(reportGroupIds)

        val dtoPage = result.map { reportGroup ->

            val info = resolveTargetInfo(
                targetType = reportGroup.targetType,
                targetId = reportGroup.targetId,
                postMap = postMap,
                commentMap = commentMap,
                memberMap = memberMap
            )
            val reportGroupId = reportGroup.requiredReportGroupId
            val reasonTypes = reasonTypeMap[reportGroupId].orEmpty()

            ReportGroupResponseDTO(
                reportGroup.targetType,
                reportGroup.targetId,
                info.nickname,
                info.title,
                info.content,
                reportGroup.reportCount,
                reasonTypes,
                reportGroup.status.toReportStatus(),
                reportGroup.latestReportedAt
            )
        }
        log.info(
            "관리자 신고 그룹 목록 조회 완료 - status={}, totalElements={}, totalPages={}, count={}",
            status,
            dtoPage.totalElements,
            dtoPage.totalPages,
            dtoPage.numberOfElements
        )
        return dtoPage
    }

    private fun validateGroupedReportSearch(
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ) {

        if (!from.isBefore(to)) {
            throw ApiException(ErrorCode.BAD_REQUEST)
        }

        if (from.plusDays(MAX_GROUP_RANGE_DAYS.toLong()).isBefore(to)) {
            throw ApiException(ErrorCode.BAD_REQUEST)
        }

        if (pageable.pageSize > MAX_GROUP_PAGE_SIZE) {
            throw ApiException(ErrorCode.BAD_REQUEST)
        }

        val unsupportedSort = pageable.sort.any { order ->
            order.property != GROUP_SORT_PROPERTY ||
                    order.direction != Sort.Direction.DESC
        }

        if (unsupportedSort) {
            throw ApiException(ErrorCode.BAD_REQUEST)
        }
    }

    private fun resolveTargetInfo(
        targetType: TargetType,
        targetId: Long,
        postMap: Map<Long, Post>,
        commentMap: Map<Long, Comment>,
        memberMap: Map<Long, Member>
    ): ReportTargetHandler.TargetInfo {

        return when (targetType) {

            TargetType.POST -> {

                val post = postMap[targetId]

                if (post == null) {

                    log.warn(
                        "신고 대상 게시글 정보 조회 실패 - targetId={}",
                        targetId
                    )

                    return ReportTargetHandler.TargetInfo(
                        null,
                        null,
                        null
                    )
                }

                ReportTargetHandler.TargetInfo(
                    post.member.nickname,
                    post.title,
                    post.content
                )
            }

            TargetType.COMMENT -> {

                val comment = commentMap[targetId]

                if (comment == null) {

                    log.warn(
                        "신고 대상 댓글 정보 조회 실패 - targetId={}",
                        targetId
                    )

                    return ReportTargetHandler.TargetInfo(
                        null,
                        null,
                        null
                    )
                }

                val member = memberMap[comment.writerId]

                ReportTargetHandler.TargetInfo(
                    member?.nickname,
                    null,
                    comment.content
                )
            }
        }
    }

    fun approveReport(
        adminId: Long,
        dto: AdminReportRequestDTO
    ) {

        log.info(
            "관리자 신고 단건 승인 시작 - adminId={}, reportId={}, targetType={}, sanctionType={}, suspensionDays={}",
            adminId,
            dto.reportId,
            dto.targetType,
            dto.sanctionType,
            dto.suspensionDays
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)

        val report = findReportOrThrow(dto.reportId)

        validatePendingStatus(report)
        validateTargetExists(report.targetType, report.targetId)
        validateSanctionDetails(dto)

        report.processReport(admin)

        reportTargetHandler.handleApproved(
            report.targetType,
            report.targetId,
            admin,
            dto.sanctionType,
            dto.suspensionDays
        )

        log.info(
            "관리자 신고 단건 승인 완료 - adminId={}, reportId={}, targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
            adminId,
            dto.reportId,
            report.targetType,
            report.targetId,
            dto.sanctionType,
            dto.suspensionDays
        )
    }

    fun rejectReport(
        adminId: Long,
        dto: AdminReportRequestDTO
    ) {

        log.info(
            "관리자 신고 단건 반려 시작 - adminId={}, reportId={}, targetType={}",
            adminId,
            dto.reportId,
            dto.targetType
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)

        val report = findReportOrThrow(dto.reportId)

        validatePendingStatus(report)

        report.rejectReport(admin)

        reportTargetHandler.handleRejected(
            report.targetType,
            report.targetId,
            admin
        )

        log.info(
            "관리자 신고 단건 반려 완료 - adminId={}, reportId={}, targetType={}, targetId={}",
            adminId,
            dto.reportId,
            report.targetType,
            report.targetId
        )
    }

    @Deprecated(
        message = "Use approveReportGroupById(adminId, reportGroupId, request) instead."
    )
    @Transactional
    // 구 API 호환을 위해 남겨둔다. 신규 승인 처리는 reportGroupId 기반 메서드로만 확장한다.
    fun approveReportGroup(
        adminId: Long,
        dto: AdminReportRequestDTO
    ) {

        log.info(
            "관리자 신고 그룹 승인 시작 - adminId={}, targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
            adminId,
            dto.targetType,
            dto.reportId,
            dto.sanctionType,
            dto.suspensionDays
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)

        val targetType = dto.targetType
        val targetId = dto.reportId

        validateTargetExists(targetType, targetId)
        validateSanctionDetails(dto)

        val updatedCount = reportRepository.updateStatusGroup(
            targetType,
            targetId,
            admin,
            ReportStatus.RESOLVED,
            ReportStatus.PENDING
        )

        log.info(
            "관리자 신고 그룹 상태 변경 결과 - adminId={}, targetType={}, targetId={}, updatedCount={}",
            adminId,
            targetType,
            targetId,
            updatedCount
        )

        if (updatedCount == 0) {

            log.warn(
                "관리자 신고 그룹 승인 실패 - 처리 가능한 PENDING 신고 없음, adminId={}, targetType={}, targetId={}",
                adminId,
                targetType,
                targetId
            )

            throw ApiException(
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
        }

        reportTargetHandler.handleApproved(
            targetType,
            targetId,
            admin,
            dto.sanctionType,
            dto.suspensionDays
        )

        log.info(
            "관리자 신고 그룹 승인 완료 - adminId={}, targetType={}, targetId={}, updatedCount={}, sanctionType={}, suspensionDays={}",
            adminId,
            targetType,
            targetId,
            updatedCount,
            dto.sanctionType,
            dto.suspensionDays
        )
    }

    @Deprecated(
        message = "Use rejectReportGroupById(adminId, reportGroupId, request) instead."
    )
    @Transactional
    // 구 API 호환을 위해 남겨둔다. 신규 반려 처리는 reportGroupId 기반 메서드로만 확장한다.
    fun rejectReportGroup(
        adminId: Long,
        dto: AdminReportRequestDTO
    ) {

        log.info(
            "관리자 신고 그룹 반려 시작 - adminId={}, targetType={}, targetId={}",
            adminId,
            dto.targetType,
            dto.reportId
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)

        val targetType = dto.targetType
        val targetId = dto.reportId

        val updatedCount = reportRepository.updateStatusGroup(
            targetType,
            targetId,
            admin,
            ReportStatus.REJECTED,
            ReportStatus.PENDING
        )

        log.info(
            "관리자 신고 그룹 반려 상태 변경 결과 - adminId={}, targetType={}, targetId={}, updatedCount={}",
            adminId,
            targetType,
            targetId,
            updatedCount
        )

        if (updatedCount == 0) {

            log.warn(
                "관리자 신고 그룹 반려 실패 - 처리 가능한 PENDING 신고 없음, adminId={}, targetType={}, targetId={}",
                adminId,
                targetType,
                targetId
            )

            throw ApiException(
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
        }

        reportTargetHandler.handleRejected(
            targetType,
            targetId,
            admin
        )

        log.info(
            "관리자 신고 그룹 반려 완료 - adminId={}, targetType={}, targetId={}, updatedCount={}",
            adminId,
            targetType,
            targetId,
            updatedCount
        )
    }

    @Transactional
    fun approveReportGroupById(
        adminId: Long,
        reportGroupId: Long,
        request: ApproveReportGroupRequest
    ) {

        log.info(
            "관리자 신고 그룹 승인 시작 - adminId={}, reportGroupId={}, sanctionType={}, suspensionDays={}",
            adminId,
            reportGroupId,
            request.sanctionType,
            request.suspensionDays
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)
        validateSanctionDetails(
            request.sanctionType,
            request.suspensionDays
        )

        val reportGroup = findReportGroupOrThrow(reportGroupId)
        val beforeStatus = reportGroup.status
        val now = LocalDateTime.now()

        validateTargetExists(
            reportGroup.targetType,
            reportGroup.targetId
        )

        reportGroup.approve(
            admin = admin,
            note = request.adminNote,
            sanctionType = request.sanctionType,
            suspensionDays = request.suspensionDays,
            now = now
        )
        reportGroupRepository.saveAndFlush(reportGroup)
        reportGroupActionRepository.save(
            ReportGroupAction.approve(
                reportGroup = reportGroup,
                admin = admin,
                beforeStatus = beforeStatus,
                note = request.adminNote,
                sanctionType = request.sanctionType,
                suspensionDays = request.suspensionDays,
                now = now
            )
        )

        val updatedCount = reportRepository.updateStatusByReportGroupId(
            reportGroupId,
            admin,
            ReportStatus.RESOLVED,
            ReportStatus.PENDING,
            now
        )

        if (updatedCount == 0) {
            log.warn(
                "관리자 신고 그룹 승인 실패 - 처리 가능한 PENDING 신고 없음, adminId={}, reportGroupId={}",
                adminId,
                reportGroupId
            )

            throw ApiException(
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
        }

        reportTargetHandler.handleApproved(
            reportGroup.targetType,
            reportGroup.targetId,
            admin,
            request.sanctionType,
            request.suspensionDays
        )

        log.info(
            "관리자 신고 그룹 승인 완료 - adminId={}, reportGroupId={}, targetType={}, targetId={}, updatedCount={}",
            adminId,
            reportGroupId,
            reportGroup.targetType,
            reportGroup.targetId,
            updatedCount
        )
    }

    @Transactional
    fun rejectReportGroupById(
        adminId: Long,
        reportGroupId: Long,
        request: RejectReportGroupRequest
    ) {

        log.info(
            "관리자 신고 그룹 반려 시작 - adminId={}, reportGroupId={}",
            adminId,
            reportGroupId
        )

        val admin = findMemberOrThrow(adminId)

        validateAdminRole(admin)

        val reportGroup = findReportGroupOrThrow(reportGroupId)
        val beforeStatus = reportGroup.status
        val now = LocalDateTime.now()

        reportGroup.reject(
            admin = admin,
            note = request.adminNote,
            now = now
        )
        reportGroupRepository.saveAndFlush(reportGroup)
        reportGroupActionRepository.save(
            ReportGroupAction.reject(
                reportGroup = reportGroup,
                admin = admin,
                beforeStatus = beforeStatus,
                note = request.adminNote,
                now = now
            )
        )

        val updatedCount = reportRepository.updateStatusByReportGroupId(
            reportGroupId,
            admin,
            ReportStatus.REJECTED,
            ReportStatus.PENDING,
            now
        )

        if (updatedCount == 0) {
            log.warn(
                "관리자 신고 그룹 반려 실패 - 처리 가능한 PENDING 신고 없음, adminId={}, reportGroupId={}",
                adminId,
                reportGroupId
            )

            throw ApiException(
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
        }

        reportTargetHandler.handleRejected(
            reportGroup.targetType,
            reportGroup.targetId,
            admin
        )

        log.info(
            "관리자 신고 그룹 반려 완료 - adminId={}, reportGroupId={}, targetType={}, targetId={}, updatedCount={}",
            adminId,
            reportGroupId,
            reportGroup.targetType,
            reportGroup.targetId,
            updatedCount
        )
    }

    private fun validateAdminRole(member: Member) {

        if (!member.isAdmin()) {

            log.warn(
                "관리자 권한 검증 실패 - userId={}",
                member.userId
            )

            throw ApiException(
                ReportErrorCode.REPORT_403_UNAUTHORIZED_ADMIN
            )
        }
    }

    private fun validateTargetExists(
        type: TargetType,
        targetId: Long
    ) {

        if (!reportTargetHandler.exists(type, targetId)) {

            log.warn(
                "신고 대상 존재 검증 실패 - targetType={}, targetId={}",
                type,
                targetId
            )

            throw ApiException(
                ReportErrorCode.REPORT_404_TARGET
            )
        }
    }

    private fun validateSanctionDetails(
        dto: AdminReportRequestDTO
    ) {

        validateSanctionDetails(
            dto.sanctionType,
            dto.suspensionDays
        )
    }

    private fun validateSanctionDetails(
        sanctionType: SanctionType?,
        suspensionDays: Int?
    ) {

        if (
            sanctionType == SanctionType.SUSPENDED &&
            (suspensionDays == null || suspensionDays <= 0)
        ) {

            log.warn(
                "신고 제재 파라미터 검증 실패 - sanctionType={}, suspensionDays={}",
                sanctionType,
                suspensionDays
            )

            throw ApiException(
                ReportErrorCode.REPORT_400_INVALID_SANCTION_PARAMETER
            )
        }
    }

    private fun validatePendingStatus(report: Report) {

        if (report.status != ReportStatus.PENDING) {

            log.warn(
                "신고 상태 검증 실패 - reportId={}, currentStatus={}",
                report.reportId,
                report.status
            )

            throw ApiException(
                ReportErrorCode.REPORT_409_ALREADY_REPORT
            )
        }
    }

    private fun findReportOrThrow(
        reportId: Long
    ): Report {

        return reportRepository.findByIdOrNull(reportId)
            ?: throw ApiException(ReportErrorCode.REPORT_404_REPORT)
    }

    private fun findReportGroupOrThrow(
        reportGroupId: Long
    ): ReportGroup {

        return reportGroupRepository.findByIdOrNull(reportGroupId)
            ?: throw ApiException(ReportErrorCode.REPORT_404_REPORT_GROUP)
    }

    private fun findMemberOrThrow(
        userId: Long
    ): Member {

        return memberRepository.findByIdOrNull(userId)
            ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)
    }

    private fun Array<out Any?>.toGroupRow(): GroupRow {

        return GroupRow(
            targetType = this[0] as TargetType,
            targetId = this[1] as Long,
            reportCount = this[2] as Long,
            latestCreatedAt = this[3] as LocalDateTime
        )
    }

    private data class GroupRow(
        val targetType: TargetType,
        val targetId: Long,
        val reportCount: Long,
        val latestCreatedAt: LocalDateTime
    )

    companion object {
        private val log = LoggerFactory.getLogger(AdminReportService::class.java)

        private const val MAX_GROUP_PAGE_SIZE = 100
        private const val DEFAULT_GROUP_LOOKBACK_DAYS = 30
        private const val MAX_GROUP_RANGE_DAYS = 90
        private const val GROUP_SORT_PROPERTY = "latestCreatedAt"
    }

    private fun ReportStatus?.toReportGroupStatus(): ReportGroupStatus? {
        return when (this) {
            null -> null
            ReportStatus.PENDING -> ReportGroupStatus.OPEN
            ReportStatus.RESOLVED -> ReportGroupStatus.APPROVED
            ReportStatus.REJECTED -> ReportGroupStatus.REJECTED
        }
    }

    private fun ReportGroupStatus.toReportStatus(): ReportStatus {
        return when (this) {
            ReportGroupStatus.OPEN -> ReportStatus.PENDING
            ReportGroupStatus.APPROVED -> ReportStatus.RESOLVED
            ReportGroupStatus.REJECTED -> ReportStatus.REJECTED
        }
    }

    private fun toReportGroupPageable(pageable: Pageable): Pageable {
        return PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(Sort.Direction.DESC, "latestReportedAt")
        )
    }

    private fun loadReasonTypesByReportGroupIds(
        reportGroupIds: List<Long>
    ): Map<Long, List<String>> {

        if (reportGroupIds.isEmpty()) {
            return emptyMap()
        }

        return reportRepository.findReasonStatsByReportGroupIds(reportGroupIds)
            .groupBy { it.reportGroupId }
            .mapValues { (_, stats) ->
                stats.map { it.reasonType }
            }
    }

    private val Post.requiredPostId: Long
        get() = postId
            ?: throw IllegalStateException("Persisted post id is required for report group mapping")

    private val Comment.requiredCommentId: Long
        get() = id
            ?: throw IllegalStateException("Persisted comment id is required for report group mapping")

    private val Comment.writerId: Long
        get() = getUserId()

    private val Member.requiredUserId: Long
        get() = userId
            ?: throw IllegalStateException("Persisted member id is required for report group mapping")

    private val ReportGroup.requiredReportGroupId: Long
        get() = reportGroupId
            ?: throw IllegalStateException("Persisted report group id is required for reason mapping")
}


