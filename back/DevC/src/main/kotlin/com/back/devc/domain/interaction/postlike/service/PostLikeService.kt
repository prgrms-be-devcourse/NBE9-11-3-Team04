package com.back.devc.domain.interaction.postLike.service

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse
import com.back.devc.domain.interaction.postLike.entity.PostLike
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.util.MemberDisplayUtil
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.errorCode.PostLikeErrorCode
import com.back.devc.global.response.PageResponse
import com.back.devc.global.response.successCode.PostLikeSuccessCode
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val notificationService: NotificationService,
) {

    /**
     * 게시글 좋아요 추가
     *
     * 동시성 처리 방식:
     * - exists 확인 후 save 하지 않음
     * - DB unique constraint + insert ignore 사용
     * - 실제 insert가 성공한 경우에만 likeCount 증가
     */
    @Transactional
    fun createLike(command: PostLikeCommand): PostLikeResponse {
        val userId = command.userId
        val postId = command.postId

        validateMemberExists(userId)
        validatePostExists(postId)

        val insertedCount = postLikeRepository.insertIgnore(userId, postId)

        if (insertedCount > 0) {
            postRepository.increaseLikeCount(postId)
            notificationService.createPostLikeNotification(postId, userId)
        }

        val likeCount = postRepository.findLikeCountByPostId(postId).toLong()

        val successCode = if (insertedCount > 0) {
            PostLikeSuccessCode.POST_LIKE_CREATED
        } else {
            PostLikeSuccessCode.POST_LIKE_ALREADY_EXISTS
        }

        return PostLikeResponse(
            postId = postId,
            liked = true,
            likeCount = likeCount,
            message = successCode.message,
        )
    }

    /**
     * 게시글 좋아요 취소
     *
     * 동시성 처리 방식:
     * - 좋아요 엔티티 조회 후 delete 하지 않음
     * - delete query의 affected row 수를 기준으로 count 감소
     * - 실제 삭제된 경우에만 likeCount 감소
     */
    @Transactional
    fun cancelLike(command: PostLikeCommand): PostLikeResponse {
        val userId = command.userId
        val postId = command.postId

        validateMemberExists(userId)
        validatePostExists(postId)

        val deletedCount = postLikeRepository.deleteByUserIdAndPostId(userId, postId)

        if (deletedCount > 0) {
            postRepository.decreaseLikeCount(postId)
        }

        val likeCount = postRepository.findLikeCountByPostId(postId).toLong()

        val successCode = if (deletedCount > 0) {
            PostLikeSuccessCode.POST_LIKE_CANCELED
        } else {
            PostLikeSuccessCode.POST_LIKE_ALREADY_CANCELED
        }

        return PostLikeResponse(
            postId = postId,
            liked = false,
            likeCount = likeCount,
            message = successCode.message,
        )
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회 - 기존 List 방식
     */
    fun getLikedPosts(query: LikedPostsQuery): List<LikedPostResponse> {
        val member = findMemberById(query.userId)

        val postLikes = postLikeRepository.findAllByMemberAndPost_IsDeletedFalse(member)

        return postLikes.map { postLike ->
            toLikedPostResponse(postLike, query.userId)
        }
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회 - 페이징 방식
     */
    fun getLikedPosts(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<LikedPostResponse> {
        val member = findMemberById(userId)

        val postLikes = postLikeRepository.findAllByMemberAndPost_IsDeletedFalse(
            member,
            pageable,
        )

        val responses = postLikes.map { postLike ->
            toLikedPostResponse(postLike, userId)
        }

        return PageResponse.from(responses)
    }

    /**
     * 현재 로그인한 사용자가 특정 게시글에 좋아요했는지 확인
     */
    fun isLikedByUser(
        userId: Long,
        postId: Long,
    ): Boolean {
        return postLikeRepository.existsByMember_UserIdAndPost_PostId(userId, postId)
    }

    private fun toLikedPostResponse(
        postLike: PostLike,
        userId: Long,
    ): LikedPostResponse {
        val post: Post = postLike.post
        val postId = post.postId ?: throw IllegalStateException("Post ID cannot be null")

        val bookmarked = bookmarkRepository.existsByMember_UserIdAndPost_PostId(
            userId,
            postId,
        )

        return LikedPostResponse(
            postId = postId,
            title = post.title,
            authorNickname = MemberDisplayUtil.getDisplayName(post.member),
            likeCount = post.likeCount.toLong(),
            commentCount = post.commentCount.toLong(),
            viewCount = post.viewCount.toLong(),
            createdAt = post.createdAt,
            liked = true,
            bookmarked = bookmarked,
        )
    }

    /**
     * 회원 조회 공통 메서드
     * 목록 조회처럼 Member 엔티티가 필요한 경우 사용
     */
    private fun findMemberById(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                EntityNotFoundException(
                    PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.code
                )
            }
    }

    /**
     * 좋아요 생성/취소에서는 엔티티 조회 대신 존재 여부만 검증한다.
     */
    private fun validateMemberExists(userId: Long) {
        if (!memberRepository.existsById(userId)) {
            throw EntityNotFoundException(
                PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.code
            )
        }
    }

    /**
     * 좋아요 생성/취소 대상 게시글 존재 여부 검증
     */
    private fun validatePostExists(postId: Long) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty()) {
            throw EntityNotFoundException(
                PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.code
            )
        }
    }
}