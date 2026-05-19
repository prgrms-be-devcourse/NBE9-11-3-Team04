package com.back.devc.domain.member.member.service

import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse
import com.back.devc.domain.member.member.dto.AdmMemberListRequest
import com.back.devc.domain.member.member.dto.AdmMemberListResponse
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.exception.errorCode.MemberErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdmMemberService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
) {

    fun getMembers(request: AdmMemberListRequest): Page<AdmMemberListResponse> {
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "createdAt"),
        )
        val members = findMembers(request, pageable)
        val userIds = members.content.mapNotNull { it.userId }

        if (userIds.isEmpty()) {
            return members.map { member -> AdmMemberListResponse.of(member, 0L, 0L) }
        }

        val postCountMap = postRepository.countPostsByUserIds(userIds)
            .associate { it.userId to it.count }
        val commentCountMap = commentRepository.countCommentsByUserIds(userIds)
            .associate { it.userId to it.count }

        return members.map { member ->
            val userId = requireNotNull(member.userId) { "Member userId must not be null" }
            AdmMemberListResponse.of(
                member = member,
                postCount = postCountMap[userId] ?: 0L,
                commentCount = commentCountMap[userId] ?: 0L,
            )
        }
    }

    fun getMemberDetail(userId: Long): AdmMemberDetailResponse {
        val member = findMemberOrThrow(userId)
        validateActiveMember(member)

        return AdmMemberDetailResponse.of(
            member = member,
            postCount = findPostCount(userId),
            commentCount = findCommentCount(userId),
        )
    }

    @Transactional
    fun updateMemberStatus(
        userId: Long,
        request: AdmMemberStatusUpdateRequest,
    ): AdmMemberDetailResponse {
        val member = findMemberOrThrow(userId)
        validateUpdatableMember(member)

        member.updateStatus(request.status)

        return AdmMemberDetailResponse.of(
            member = member,
            postCount = findPostCount(userId),
            commentCount = findCommentCount(userId),
        )
    }

    private fun findMembers(request: AdmMemberListRequest, pageable: Pageable): Page<Member> {
        val keyword = request.keyword?.takeIf { it.isNotBlank() }
        val status = request.status

        if (keyword == null && status == null) {
            return memberRepository.findAll(pageable)
        }

        if (keyword != null && status == null) {
            return memberRepository.findByNicknameContainingOrEmailContaining(keyword, keyword, pageable)
        }

        if (keyword == null && status != null) {
            return memberRepository.findByStatus(status, pageable)
        }

        return memberRepository.findByStatusAndNicknameContainingOrStatusAndEmailContaining(
            status1 = requireNotNull(status),
            nickname = requireNotNull(keyword),
            status2 = status,
            email = keyword,
            pageable = pageable,
        )
    }

    private fun findMemberOrThrow(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow { ApiException(ErrorCode.MEMBER_NOT_FOUND) }
    }

    private fun findPostCount(userId: Long): Long {
        return postRepository.countPostsByUserIds(listOf(userId))
            .firstOrNull()
            ?.count
            ?: 0L
    }

    private fun findCommentCount(userId: Long): Long {
        return commentRepository.countCommentsByUserIds(listOf(userId))
            .firstOrNull()
            ?.count
            ?: 0L
    }

    private fun validateActiveMember(member: Member) {
        when (member.status) {
            MemberStatus.WITHDRAWN -> throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)
            MemberStatus.BLACKLISTED -> throw ApiException(AuthErrorCode.MEMBER_BLACKLISTED)
            else -> Unit
        }
    }

    private fun validateUpdatableMember(member: Member) {
        if (member.status == MemberStatus.WITHDRAWN) {
            throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)
        }
    }
}

