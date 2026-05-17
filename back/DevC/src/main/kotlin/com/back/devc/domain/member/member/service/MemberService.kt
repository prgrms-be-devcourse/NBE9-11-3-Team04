package com.back.devc.domain.member.member.service

import com.back.devc.domain.member.member.dto.MemberWithdrawResponse
import com.back.devc.domain.member.member.dto.MyInfoResponse
import com.back.devc.domain.member.member.dto.PublicProfilePostResponse
import com.back.devc.domain.member.member.dto.PublicProfileResponse
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.MemberErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository
) {

    private val log = LoggerFactory.getLogger(MemberService::class.java)

    // 내 정보 조회
    @Transactional(readOnly = true)
    fun getMyInfo(userId: Long): MyInfoResponse {
        log.info("내 정보 조회 시작 - userId={}", userId)

        val member = findMemberOrThrow(userId)
        val memberId = member.userId ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)
        val createdAt = member.createdAt ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)

        log.info("내 정보 조회 완료 - userId={}, email={}", memberId, member.email)

        return MyInfoResponse(
            userId = memberId,
            email = member.email,
            nickname = member.nickname,
            role = member.role,
            status = member.status,
            createdAt = createdAt
        )
    }

    // 공개 프로필 조회 (최근 게시글 20개 포함)
    @Transactional(readOnly = true)
    fun getPublicProfile(userId: Long): PublicProfileResponse {
        log.info("공개 프로필 조회 시작 - userId={}", userId)

        val member = findMemberOrThrow(userId)
        val memberId = member.userId ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)

        val posts = postRepository
            .findTop20ByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member)
            .map { post ->
                PublicProfilePostResponse(
                    postId = post.postId,
                    title = post.title,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    createdAt = post.createdAt
                )
            }

        log.info("공개 프로필 조회 완료 - userId={}, postCount={}", memberId, posts.size)

        return PublicProfileResponse(
            userId = memberId,
            nickname = member.nickname,
            posts = posts
        )
    }

    // 회원 탈퇴 처리
    @Transactional
    fun withdraw(userId: Long): MemberWithdrawResponse {
        log.info("회원 탈퇴 시작 - userId={}", userId)

        val member = findMemberOrThrow(userId)
        val memberId = member.userId ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)

        member.withdraw()

        log.info("회원 탈퇴 완료 - userId={}", memberId)

        return MemberWithdrawResponse(memberId)
    }

    // 회원 조회 공통 처리
    private fun findMemberOrThrow(userId: Long): Member {
        return memberRepository.findByIdOrNull(userId)
            ?: run {
                log.warn("회원 조회 실패 - 회원 없음, userId={}", userId)
                throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)
            }
    }
}