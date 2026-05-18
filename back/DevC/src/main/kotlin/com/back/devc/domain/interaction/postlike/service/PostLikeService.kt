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
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.PostLikeErrorCode
import com.back.devc.global.response.PageResponse
import com.back.devc.global.response.successCode.PostLikeSuccessCode
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

    // 게시글 좋아요 추가
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

    // 게시글 좋아요 취소
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

    // 좋아요한 게시글 목록 조회 - List
    fun getLikedPosts(query: LikedPostsQuery): List<LikedPostResponse> {
        val member = findMemberById(query.userId)
        val postLikes = postLikeRepository.findAllByMemberAndPost_IsDeletedFalse(member)

        return postLikes.map { postLike ->
            toLikedPostResponse(postLike, query.userId)
        }
    }

    // 좋아요한 게시글 목록 조회 - Paging
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

    // 좋아요 여부 확인
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
        val postId = post.postId
            ?: throw ApiException(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND)

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

    private fun findMemberById(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                ApiException(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND)
            }
    }

    private fun validateMemberExists(userId: Long) {
        if (!memberRepository.existsById(userId)) {
            throw ApiException(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND)
        }
    }

    private fun validatePostExists(postId: Long) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty) {
            throw ApiException(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND)
        }
    }
}