package com.back.devc.domain.post.comment.service;

import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.util.MemberDisplayUtil;
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService;
import com.back.devc.domain.post.comment.dto.CommentCreateRequest;
import com.back.devc.domain.post.comment.dto.CommentDeleteResponse;
import com.back.devc.domain.post.comment.dto.CommentListResponse;
import com.back.devc.domain.post.comment.dto.CommentResponse;
import com.back.devc.domain.post.comment.dto.CommentUpdateRequest;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.domain.post.post.service.PostService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.CommentErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final MemberRepository memberRepository;
    private final CommentAttachmentService commentAttachmentService;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse createComment(Long postId, Long loginUserId, CommentCreateRequest request) {
        log.info("댓글 작성 시작 - postId={}, loginUserId={}", postId, loginUserId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("댓글 작성 실패 - 게시글 없음, postId={}", postId);
                    return new ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND);
                });

        Member member = memberRepository.findById(loginUserId)
                .orElseThrow(() -> {
                    log.warn("댓글 작성 실패 - 회원 없음, loginUserId={}", loginUserId);
                    return new ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND);
                });

        Comment comment = Comment.create(
                postId,
                member.getUserId(),
                null,
                request.content()
        );
        Comment savedComment = commentRepository.save(comment);
        postService.increaseCommentCount(postId);
        log.info("댓글 저장 완료 - commentId={}, postId={}, loginUserId={}", savedComment.getId(), postId, loginUserId);
        notificationService.createCommentNotification(postId, loginUserId, savedComment.getId());
        log.info("댓글 알림 처리 요청 완료 - commentId={}, postId={}", savedComment.getId(), postId);

        return toResponse(savedComment, post.getTitle(), MemberDisplayUtil.getDisplayName(member));
    }

    @Transactional
    public CommentResponse createReply(Long parentCommentId, Long loginUserId, CommentCreateRequest request) {
        log.info("대댓글 작성 시작 - parentCommentId={}, loginUserId={}", parentCommentId, loginUserId);
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> {
                    log.warn("대댓글 작성 실패 - 부모 댓글 없음, parentCommentId={}", parentCommentId);
                    return new ApiException(CommentErrorCode.COMMENT_404_PARENT_NOT_FOUND);
                });

        if (parentComment.isDeleted()) {
            log.warn("대댓글 작성 실패 - 삭제된 부모 댓글, parentCommentId={}", parentCommentId);
            throw new ApiException(CommentErrorCode.COMMENT_400_REPLY_TO_DELETED_COMMENT);
        }

        Post post = postRepository.findById(parentComment.getPostId())
                .orElseThrow(() -> {
                    log.warn("대댓글 작성 실패 - 게시글 없음, postId={}", parentComment.getPostId());
                    return new ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND);
                });

        Member member = memberRepository.findById(loginUserId)
                .orElseThrow(() -> {
                    log.warn("대댓글 작성 실패 - 회원 없음, loginUserId={}", loginUserId);
                    return new ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND);
                });

        Comment reply = Comment.create(
                parentComment.getPostId(),
                member.getUserId(),
                parentComment.getId(),
                request.content()
        );
        Comment savedReply = commentRepository.save(reply);
        postService.increaseCommentCount(parentComment.getPostId());
        log.info("대댓글 저장 완료 - replyCommentId={}, parentCommentId={}, loginUserId={}", savedReply.getId(), parentCommentId, loginUserId);
        notificationService.createReplyNotification(parentCommentId, loginUserId, savedReply.getId());
        log.info("답글 알림 처리 요청 완료 - replyCommentId={}, parentCommentId={}", savedReply.getId(), parentCommentId);

        return toResponse(savedReply, post.getTitle(), MemberDisplayUtil.getDisplayName(member));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long loginUserId, CommentUpdateRequest request) {
        log.info("댓글 수정 시작 - commentId={}, loginUserId={}", commentId, loginUserId);
        Comment comment = findComment(commentId);
        validateOwner(comment, loginUserId);

        comment.updateContent(request.content());
        log.info("댓글 수정 완료 - commentId={}, loginUserId={}", commentId, loginUserId);

        String postTitle = findPostTitle(comment.getPostId());
        String nickname = findMemberNickname(comment.getUserId());

        return toResponse(comment, postTitle, nickname);
    }

    @Transactional
    public CommentDeleteResponse deleteComment(Long commentId, Long loginUserId) {
        Comment comment = findComment(commentId);
        validateOwner(comment, loginUserId);

        if (!comment.isDeleted()) {
            comment.softDelete();
            postService.decreaseCommentCount(comment.getPostId());
            log.info("댓글 삭제 완료 - commentId={}, postId={}, loginUserId={}", commentId, comment.getPostId(), loginUserId);
        }

        return new CommentDeleteResponse(commentId, "댓글 삭제 성공");
    }

    public CommentListResponse getComments(Long postId) {
        log.info("댓글 목록 조회 시작 - postId={}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("댓글 목록 조회 실패 - 게시글 없음, postId={}", postId);
                    return new ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND);
                });

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        log.info("댓글 목록 조회 완료 - postId={}, count={}", postId, comments.size());
        List<CommentResponse> responses = comments.stream()
                .map(comment -> toResponse(comment, post.getTitle(), findMemberNickname(comment.getUserId())))
                .toList();

        return new CommentListResponse(buildCommentHierarchy(responses));
    }

    private List<CommentResponse> buildCommentHierarchy(List<CommentResponse> allComments) {
        Map<Long, CommentResponse> commentMap = new LinkedHashMap<>();
        List<CommentResponse> parentComments = new ArrayList<>();

        for (CommentResponse commentResponse : allComments) {
            commentMap.put(commentResponse.commentId(), commentResponse);
        }

        for (CommentResponse commentResponse : allComments) {
            if (commentResponse.parentCommentId() == null) {
                parentComments.add(commentResponse);
            } else {
                CommentResponse parent = commentMap.get(commentResponse.parentCommentId());
                if (parent != null) {
                    parent.replies().add(commentResponse);
                }
            }
        }

        return parentComments;
    }

    private CommentResponse toResponse(Comment comment, String postTitle, String nickname) {
        CommentResponse response = CommentResponse.of(
                comment.getId(),
                comment.getPostId(),
                postTitle,
                comment.getUserId(),
                nickname,
                comment.getParentCommentId(),
                comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );

        if (!comment.isDeleted()) {
            response.attachments().addAll(
                    commentAttachmentService.getAttachments(comment.getId()).attachments()
            );
        }

        return response;
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("댓글 조회 실패 - 댓글 없음, commentId={}", commentId);
                    return new ApiException(CommentErrorCode.COMMENT_404_NOT_FOUND);
                });
    }

    private void validateOwner(Comment comment, Long loginUserId) {
        if (!comment.getUserId().equals(loginUserId)) {
            log.warn("댓글 권한 검증 실패 - commentId={}, ownerUserId={}, loginUserId={}", comment.getId(), comment.getUserId(), loginUserId);
            throw new ApiException(CommentErrorCode.COMMENT_403_FORBIDDEN);
        }
    }

    private String findPostTitle(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("게시글 제목 조회 실패 - 게시글 없음, postId={}", postId);
                    return new ApiException(CommentErrorCode.COMMENT_404_POST_NOT_FOUND);
                })
                .getTitle();
    }

    private String findMemberNickname(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 닉네임 조회 실패 - 회원 없음, userId={}", userId);
                    return new ApiException(CommentErrorCode.COMMENT_404_MEMBER_NOT_FOUND);
                });

        return MemberDisplayUtil.getDisplayName(member);
    }
}