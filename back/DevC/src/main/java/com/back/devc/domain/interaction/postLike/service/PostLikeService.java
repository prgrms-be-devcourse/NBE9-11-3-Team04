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
     * 이미 좋아요가 존재하면 저장하지 않고 현재 상태만 반환한다.
     * 처음 좋아요하는 경우에만 좋아요 엔티티 저장, 좋아요 수 증가, 알림 생성 수행.
     */
    @Transactional
    public PostLikeResponse createLike(PostLikeCommand command) {
        Member member = findMemberById(command.userId());
        Post post = findPostById(command.postId());

        if (postLikeRepository.existsByMemberAndPost(member, post)) {
            return buildPostLikeResponse(
                    post,
                    true,
                    PostLikeSuccessCode.POST_LIKE_ALREADY_EXISTS
            );
        }

        PostLike postLike = PostLike.create(member, post);
        postLikeRepository.save(postLike);

        post.increaseLikeCount();
        notificationService.createPostLikeNotification(post.getPostId(), member.getUserId());

        return buildPostLikeResponse(
                post,
                true,
                PostLikeSuccessCode.POST_LIKE_CREATED
        );
    }

    /**
     * 게시글 좋아요 취소
     *
     * 좋아요가 없으면 현재 상태만 반환한다.
     * 좋아요가 있으면 삭제 후 좋아요 수 감소 처리.
     */
    @Transactional
    public PostLikeResponse cancelLike(PostLikeCommand command) {
        Member member = findMemberById(command.userId());
        Post post = findPostById(command.postId());

        PostLike postLike = postLikeRepository.findByMemberAndPost(member, post)
                .orElse(null);

        if (postLike == null) {
            return buildPostLikeResponse(
                    post,
                    false,
                    PostLikeSuccessCode.POST_LIKE_ALREADY_CANCELED
            );
        }

        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();

        return buildPostLikeResponse(
                post,
                false,
                PostLikeSuccessCode.POST_LIKE_CANCELED
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
     * 서비스 내부 중복 제거용
     */
    private Member findMemberById(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode()
                ));
    }

    /**
     * 게시글 조회 공통 메서드
     * 서비스 내부 중복 제거용
     */
    private Post findPostById(Long postId) {
        return postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new EntityNotFoundException(
                        PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.getCode()
                ));
    }

    /**
     * 좋아요 응답 DTO 생성 공통 메서드
     * 응답 구조 변경 시 한 곳만 수정하면 되게 분리
     */
    private PostLikeResponse buildPostLikeResponse(Post post, boolean liked, PostLikeSuccessCode successCode) {
        return new PostLikeResponse(
                post.getPostId(),
                liked,
                post.getLikeCount(),
                successCode.getMessage()
        );
    }
}