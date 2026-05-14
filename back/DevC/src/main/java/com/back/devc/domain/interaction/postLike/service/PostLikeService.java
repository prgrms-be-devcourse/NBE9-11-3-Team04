package com.back.devc.domain.interaction.postLike.service;

import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery;
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand;
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse;
import com.back.devc.domain.interaction.postLike.entity.PostLike;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.util.MemberDisplayUtil;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.errorcode.PostLikeErrorCode;
import com.back.devc.global.response.successCode.PostLikeSuccessCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    /**
     * 게시글 좋아요 추가
     *
     * 동시성 처리 방식:
     * - exists 확인 후 save 하지 않음
     * - DB unique constraint + insert ignore 사용
     * - 실제 insert가 성공한 경우에만 likeCount 증가
     */
    @Transactional
    public PostLikeResponse createLike(PostLikeCommand command) {
        Long userId = command.userId();
        Long postId = command.postId();

        validateMemberExists(userId);
        validatePostExists(postId);

        int insertedCount = postLikeRepository.insertIgnore(userId, postId);

        if (insertedCount > 0) {
            postRepository.increaseLikeCount(postId);
            notificationService.createPostLikeNotification(postId, userId);
        }

        int likeCount = postRepository.findLikeCountByPostId(postId);

        PostLikeSuccessCode successCode = insertedCount > 0
                ? PostLikeSuccessCode.POST_LIKE_CREATED
                : PostLikeSuccessCode.POST_LIKE_ALREADY_EXISTS;

        return new PostLikeResponse(
                postId,
                true,
                likeCount,
                successCode.getMessage()
        );
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
    public PostLikeResponse cancelLike(PostLikeCommand command) {
        Long userId = command.userId();
        Long postId = command.postId();

        validateMemberExists(userId);
        validatePostExists(postId);

        int deletedCount = postLikeRepository.deleteByUserIdAndPostId(userId, postId);

        if (deletedCount > 0) {
            postRepository.decreaseLikeCount(postId);
        }

        int likeCount = postRepository.findLikeCountByPostId(postId);

        PostLikeSuccessCode successCode = deletedCount > 0
                ? PostLikeSuccessCode.POST_LIKE_CANCELED
                : PostLikeSuccessCode.POST_LIKE_ALREADY_CANCELED;

        return new PostLikeResponse(
                postId,
                false,
                likeCount,
                successCode.getMessage()
        );
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회
     */
    public List<LikedPostResponse> getLikedPosts(LikedPostsQuery query) {
        Member member = findMemberById(query.userId());

        List<PostLike> postLikes = postLikeRepository.findAllByMemberAndPost_IsDeletedFalse(member);

        return postLikes.stream()
                .map(postLike -> {
                    Post post = postLike.getPost();

                    return new LikedPostResponse(
                            post.getPostId(),
                            post.getTitle(),
                            MemberDisplayUtil.getDisplayName(post.getMember()),
                            post.getLikeCount(),
                            post.getCommentCount(),
                            post.getViewCount(),
                            post.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * 회원 조회 공통 메서드
     * 목록 조회처럼 Member 엔티티가 필요한 경우 사용
     */
    private Member findMemberById(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode()
                ));
    }

    /**
     * 좋아요 생성/취소에서는 엔티티 조회 대신 존재 여부만 검증한다.
     */
    private void validateMemberExists(Long userId) {
        if (!memberRepository.existsById(userId)) {
            throw new EntityNotFoundException(
                    PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode()
            );
        }
    }

    /**
     * 좋아요 생성/취소 대상 게시글 존재 여부 검증
     */
    private void validatePostExists(Long postId) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty()) {
            throw new EntityNotFoundException(
                    PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.getCode()
            );
        }
    }
}