package com.back.devc.domain.interaction.report.service

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
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
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime



@Service
@Transactional
class AdminReportService(
    private val reportRepository: ReportRepository,
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

            val groupRow = requireNotNull(row).toGroupRow()

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

        log.info(
            "관리자 신고 그룹 목록 조회 시작 - status={}, page={}, size={}",
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

        val rows = result.content

        log.debug(
            "관리자 신고 그룹 조회 row 수 - status={}, rowCount={}",
            status,
            rows.size
        )

        val groupRows = rows.map { row ->
            requireNotNull(row).toGroupRow()
        }

        val postIds = groupRows
            .filter { it.targetType == TargetType.POST }
            .map { it.targetId }

        val commentIds = groupRows
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
                    .associateBy { requireNotNull(it.postId) }
            }

        val commentMap: Map<Long, Comment> =
            if (commentIds.isEmpty()) {
                emptyMap()
            } else {
                commentRepository.findAllByIdIn(commentIds)
                    .associateBy { requireNotNull(it.id) }
            }

        val commentWriterIds = commentMap.values
            .map { requireNotNull(it.getUserId()) }
            .distinct()

        val memberMap: Map<Long, Member> =
            if (commentWriterIds.isEmpty()) {
                emptyMap()
            } else {
                memberRepository.findAllById(commentWriterIds)
                    .associateBy { requireNotNull(it.userId) }
            }

        val reasonTypeMap = loadReasonTypesBatch(
            postIds,
            commentIds
        )

        val dtoPage = result.map { row ->

            val groupRow = requireNotNull(row).toGroupRow()

            val info = resolveTargetInfo(
                targetType = groupRow.targetType,
                targetId = groupRow.targetId,
                postMap = postMap,
                commentMap = commentMap,
                memberMap = memberMap
            )

            val key = buildKey(
                groupRow.targetType,
                groupRow.targetId
            )

            val reasonTypes = reasonTypeMap[key] ?.filterNotNull() ?: emptyList()

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

    private fun loadReasonTypesBatch(
        postIds: List<Long>,
        commentIds: List<Long>
    ): Map<String, List<String?>> {

        log.debug(
            "신고 사유 타입 batch 조회 시작 - postCount={}, commentCount={}",
            postIds.size,
            commentIds.size
        )

        if (postIds.isEmpty() && commentIds.isEmpty()) {
            log.debug("신고 사유 타입 batch 조회 생략 - 조회 대상 없음")
            return emptyMap()
        }

        val reasonRows = reportRepository.findReasonTypesBatch(
            TargetType.POST,
            if (postIds.isEmpty()) listOf(-1L) else postIds,
            TargetType.COMMENT,
            if (commentIds.isEmpty()) listOf(-1L) else commentIds
        )

        val map = mutableMapOf<String, MutableList<String?>>()

        reasonRows.forEach { row ->

            val safeRow = requireNotNull(row)

            val type = safeRow[0] as TargetType
            val id = safeRow[1] as Long
            val reasonType = safeRow[2] as String?

            val key = buildKey(type, id)

            map.computeIfAbsent(key) {
                mutableListOf()
            }.add(reasonType)
        }

        log.debug(
            "신고 사유 타입 batch 조회 완료 - reasonRowCount={}, keyCount={}",
            reasonRows.size,
            map.size
        )

        return map
    }

    private fun buildKey(
        type: TargetType,
        id: Long
    ): String {
        return "${type.name}:$id"
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

                val member = memberMap[comment.getUserId()]

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

    @Transactional
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

    @Transactional
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

        if (
            dto.sanctionType == SanctionType.SUSPENDED &&
            (dto.suspensionDays == null || dto.suspensionDays <= 0)
        ) {

            log.warn(
                "신고 제재 파라미터 검증 실패 - sanctionType={}, suspensionDays={}",
                dto.sanctionType,
                dto.suspensionDays
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

        return reportRepository.findById(reportId)
            .orElseThrow {
                ApiException(
                    ReportErrorCode.REPORT_404_REPORT
                )
            }
    }

    private fun findMemberOrThrow(
        userId: Long
    ): Member {

        return memberRepository.findById(userId)
            .orElseThrow {
                ApiException(
                    MemberErrorCode.MEMBER_NOT_FOUND
                )
            }
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
        private val log = LoggerFactory.getLogger( AdminReportService::class.java )

        private const val MAX_GROUP_PAGE_SIZE = 100
        private const val DEFAULT_GROUP_LOOKBACK_DAYS = 30
        private const val MAX_GROUP_RANGE_DAYS = 90
        private const val GROUP_SORT_PROPERTY = "latestCreatedAt"
    }
}



