package com.back.devc.domain.post.comment.service

import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.util.MemberDisplayUtil
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService
import com.back.devc.domain.post.comment.dto.CommentCreateRequest
import com.back.devc.domain.post.comment.dto.CommentDeleteResponse
import com.back.devc.domain.post.comment.dto.CommentListResponse
import com.back.devc.domain.post.comment.dto.CommentResponse
import com.back.devc.domain.post.comment.dto.CommentUpdateRequest
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.domain.post.post.service.PostService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.CommentErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max
import kotlin.math.min

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val postService: PostService,
    private val memberRepository: MemberRepository,
    private val commentAttachmentService: CommentAttachmentService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun createComment(
        postId: Long,
        loginUserId: Long,
        request: CommentCreateRequest,
    ): CommentResponse {
        log.info("댓글 작성 시작 - postId={}, loginUserId={}", postId, loginUserId)

        val post = postRepository.findById(postId)
            .orElseThrow {
                log.warn("댓글 작성 실패 - 게시글 없음, postId={}", postId)
                ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
            }

        // 삭제된 게시글에는 댓글을 작성할 수 없도록 차단
        if (post.getIsDeleted()) {
            log.warn("댓글 작성 실패 - 삭제된 게시글, postId={}", postId)
            throw ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
        }

        val member = memberRepository.findById(loginUserId)
            .orElseThrow {
                log.warn("댓글 작성 실패 - 회원 없음, loginUserId={}", loginUserId)
                ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND)
            }

        val memberId = requireNotNull(member.userId)
        val comment = Comment.create(
            postId = postId,
            userId = memberId,
            parentCommentId = null,
            content = request.content,
        )
        val savedComment = commentRepository.save(comment)
        postService.increaseCommentCount(postId)

        val savedCommentId = requireNotNull(savedComment.id)
        log.info(
            "댓글 저장 완료 - commentId={}, postId={}, loginUserId={}",
            savedCommentId,
            postId,
            loginUserId,
        )

        // 댓글 저장 트랜잭션이 정상 커밋된 이후 알림을 생성하도록 이벤트 발행
        eventPublisher.publishEvent(CommentCreatedEvent(postId, loginUserId, savedCommentId))
        log.info("댓글 알림 이벤트 발행 완료 - commentId={}, postId={}", savedCommentId, postId)

        return toResponse(savedComment, post.getTitle(), MemberDisplayUtil.getDisplayName(member))
    }

    @Transactional
    fun createReply(
        parentCommentId: Long,
        loginUserId: Long,
        request: CommentCreateRequest,
    ): CommentResponse {
        log.info("대댓글 작성 시작 - parentCommentId={}, loginUserId={}", parentCommentId, loginUserId)

        val parentComment = commentRepository.findById(parentCommentId)
            .orElseThrow {
                log.warn("대댓글 작성 실패 - 부모 댓글 없음, parentCommentId={}", parentCommentId)
                ApiException(CommentErrorCode.COMMENT_404_PARENT_NOT_FOUND)
            }

        if (parentComment.isDeleted) {
            log.warn("대댓글 작성 실패 - 삭제된 부모 댓글, parentCommentId={}", parentCommentId)
            throw ApiException(CommentErrorCode.COMMENT_400_REPLY_TO_DELETED_COMMENT)
        }

        val parentPostId = requireNotNull(parentComment.postId)
        val post = postRepository.findById(parentPostId)
            .orElseThrow {
                log.warn("대댓글 작성 실패 - 게시글 없음, postId={}", parentPostId)
                ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
            }

        // 삭제된 게시글에는 대댓글을 작성할 수 없도록 차단
        if (post.getIsDeleted()) {
            log.warn("대댓글 작성 실패 - 삭제된 게시글, postId={}", parentPostId)
            throw ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
        }

        val member = memberRepository.findById(loginUserId)
            .orElseThrow {
                log.warn("대댓글 작성 실패 - 회원 없음, loginUserId={}", loginUserId)
                ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND)
            }

        val memberId = requireNotNull(member.userId)
        val reply = Comment.create(
            postId = parentPostId,
            userId = memberId,
            parentCommentId = requireNotNull(parentComment.id),
            content = request.content,
        )
        val savedReply = commentRepository.save(reply)
        postService.increaseCommentCount(parentPostId)

        val savedReplyId = requireNotNull(savedReply.id)
        log.info(
            "대댓글 저장 완료 - replyCommentId={}, parentCommentId={}, loginUserId={}",
            savedReplyId,
            parentCommentId,
            loginUserId,
        )

        // 대댓글 저장 트랜잭션이 정상 커밋된 이후 알림을 생성하도록 이벤트 발행
        eventPublisher.publishEvent(ReplyCreatedEvent(parentCommentId, loginUserId, savedReplyId))
        log.info(
            "답글 알림 이벤트 발행 완료 - replyCommentId={}, parentCommentId={}",
            savedReplyId,
            parentCommentId,
        )

        return toResponse(savedReply, post.getTitle(), MemberDisplayUtil.getDisplayName(member))
    }

    @Transactional
    fun updateComment(
        commentId: Long,
        loginUserId: Long,
        request: CommentUpdateRequest,
    ): CommentResponse {
        log.info("댓글 수정 시작 - commentId={}, loginUserId={}", commentId, loginUserId)
        val comment = findComment(commentId)
        validateOwner(comment, loginUserId)

        comment.updateContent(request.content)
        log.info("댓글 수정 완료 - commentId={}, loginUserId={}", commentId, loginUserId)

        val postTitle = findPostTitle(requireNotNull(comment.postId))
        val nickname = findMemberNickname(comment.getUserId())

        return toResponse(comment, postTitle, nickname)
    }

    @Transactional
    fun deleteComment(commentId: Long, loginUserId: Long): CommentDeleteResponse {
        log.info("댓글 삭제 시작 - commentId={}, loginUserId={}", commentId, loginUserId)
        val comment = findComment(commentId)
        validateOwner(comment, loginUserId)

        if (!comment.isDeleted) {
            comment.softDelete()
            val postId = requireNotNull(comment.postId)
            postService.decreaseCommentCount(postId)
            log.info(
                "댓글 삭제 완료 - commentId={}, postId={}, loginUserId={}",
                commentId,
                postId,
                loginUserId,
            )
        } else {
            log.info("댓글 삭제 요청 생략 - 이미 삭제된 댓글, commentId={}, loginUserId={}", commentId, loginUserId)
        }

        return CommentDeleteResponse(commentId, "댓글 삭제 성공")
    }

    fun getComments(postId: Long, page: Int, size: Int): CommentListResponse {
        log.info("댓글 목록 조회 시작 - postId={}, page={}, size={}", postId, page, size)

        val post = postRepository.findById(postId)
            .orElseThrow {
                log.warn("댓글 목록 조회 실패 - 게시글 없음, postId={}", postId)
                ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
            }

        // 삭제된 게시글의 댓글 목록은 조회되지 않도록 차단
        if (post.getIsDeleted()) {
            log.warn("댓글 목록 조회 실패 - 삭제된 게시글, postId={}", postId)
            throw ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
        }

        val safePage = max(page, 0)
        val safeSize = min(max(size, 1), 20)
        if (safePage != page || safeSize != size) {
            log.info(
                "댓글 목록 조회 요청값 보정 - postId={}, requestedPage={}, requestedSize={}, safePage={}, safeSize={}",
                postId,
                page,
                size,
                safePage,
                safeSize,
            )
        }
        val pageable: Pageable = PageRequest.of(safePage, safeSize)

        val commentPage = commentRepository.findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
            postId,
            pageable,
        )

        val parentComments = commentPage.content
            .map { comment ->
                toResponse(
                    comment,
                    post.getTitle(),
                    findMemberNickname(comment.getUserId()),
                )
            }

        log.info(
            "댓글 목록 조회 완료 - postId={}, page={}, size={}, count={}",
            postId,
            commentPage.number,
            commentPage.size,
            parentComments.size,
        )

        return CommentListResponse(
            parentComments,
            commentPage.number,
            commentPage.size,
            commentPage.totalElements,
            commentPage.totalPages,
            commentPage.hasNext(),
        )
    }

    private fun buildCommentHierarchy(allComments: List<CommentResponse>): List<CommentResponse> {
        val commentMap = linkedMapOf<Long, CommentResponse>()
        val parentComments = mutableListOf<CommentResponse>()

        allComments.forEach { commentResponse ->
            commentMap[commentResponse.commentId] = commentResponse
        }

        allComments.forEach { commentResponse ->
            val parentCommentId = commentResponse.parentCommentId
            if (parentCommentId == null) {
                parentComments.add(commentResponse)
            } else {
                commentMap[parentCommentId]?.replies?.add(commentResponse)
            }
        }

        return parentComments
    }

    private fun toResponse(comment: Comment, postTitle: String, nickname: String): CommentResponse {
        val commentId = requireNotNull(comment.id)
        val response = CommentResponse.of(
            commentId,
            requireNotNull(comment.postId),
            postTitle,
            comment.getUserId(),
            nickname,
            comment.parentCommentId,
            requireNotNull(comment.content),
            comment.isDeleted,
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
        )

        if (!comment.isDeleted) {
            response.attachments.addAll(
                commentAttachmentService.getAttachments(commentId).attachments,
            )
        } else {
            log.debug("삭제된 댓글 응답 변환 - 첨부파일 조회 생략, commentId={}", commentId)
        }

        return response
    }

    private fun findComment(commentId: Long): Comment {
        return commentRepository.findById(commentId)
            .orElseThrow {
                log.warn("댓글 조회 실패 - 댓글 없음, commentId={}", commentId)
                ApiException(CommentErrorCode.COMMENT_404_NOT_FOUND)
            }
    }

    private fun validateOwner(comment: Comment, loginUserId: Long) {
        if (comment.getUserId() != loginUserId) {
            log.warn(
                "댓글 권한 검증 실패 - commentId={}, ownerUserId={}, loginUserId={}",
                comment.id,
                comment.getUserId(),
                loginUserId,
            )
            throw ApiException(CommentErrorCode.COMMENT_403_FORBIDDEN)
        }
    }

    private fun findPostTitle(postId: Long): String {
        return postRepository.findById(postId)
            .orElseThrow {
                log.warn("게시글 제목 조회 실패 - 게시글 없음, postId={}", postId)
                ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND)
            }
            .getTitle()
    }

    private fun findMemberNickname(userId: Long): String {
        val member = memberRepository.findById(userId)
            .orElseThrow {
                log.warn("회원 닉네임 조회 실패 - 회원 없음, userId={}", userId)
                ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND)
            }

        return MemberDisplayUtil.getDisplayName(member)
    }

    @JvmRecord
    data class CommentCreatedEvent(
        val postId: Long,
        val actorUserId: Long,
        val commentId: Long,
    )

    @JvmRecord
    data class ReplyCreatedEvent(
        val parentCommentId: Long,
        val actorUserId: Long,
        val replyCommentId: Long,
    )

    companion object {
        private val log = LoggerFactory.getLogger(CommentService::class.java)
    }
}