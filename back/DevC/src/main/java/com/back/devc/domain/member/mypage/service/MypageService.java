package com.back.devc.domain.member.mypage.service;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.mypage.dto.MyCommentResponse;
import com.back.devc.domain.member.mypage.dto.MyPostResponse;
import com.back.devc.domain.member.mypage.dto.MyProfileResponse;
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.errorCode.MypageErrorCode;
import com.back.devc.global.response.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostLikeService postLikeService;
    private final BookmarkService bookmarkService;

    private Member getMember(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MypageErrorCode.MYPAGE_404_MEMBER_NOT_FOUND.getCode()
                ));
    }

    public MyProfileResponse getMyProfile(Long userId) {
        Member member = getMember(userId);

        return new MyProfileResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname()
        );
    }

    public PageResponse<MyPostResponse> getMyPosts(Long userId, Pageable pageable) {
        Member member = getMember(userId);

        Page<Post> posts = postRepository.findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(
                member,
                pageable
        );

        Page<MyPostResponse> response = posts.map(post -> {
            Long postId = post.getPostId();

            boolean liked = postLikeRepository.existsByMember_UserIdAndPost_PostId(
                    userId,
                    postId
            );

            boolean bookmarked = bookmarkRepository.existsByMember_UserIdAndPost_PostId(
                    userId,
                    postId
            );

            return new MyPostResponse(
                    postId,
                    post.getTitle(),
                    (long) post.getLikeCount(),
                    (long) post.getCommentCount(),
                    (long) post.getViewCount(),
                    post.getCreatedAt(),
                    liked,
                    bookmarked
            );
        });

        return PageResponse.from(response);
    }

    public PageResponse<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {
        getMember(userId);

        Page<MyCommentResponse> comments = commentRepository.findMyComments(userId, pageable);

        return PageResponse.from(comments);
    }

    public PageResponse<LikedPostResponse> getMyLikedPosts(Long userId, Pageable pageable) {
        getMember(userId);

        return postLikeService.getLikedPosts(userId, pageable);
    }

    public PageResponse<BookmarkedPostResponse> getMyBookmarkedPosts(Long userId, Pageable pageable) {
        getMember(userId);

        return bookmarkService.getBookmarkedPosts(userId, pageable);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        Member member = getMember(userId);

        String newNickname = request.getNickname().trim();

        if (!member.getNickname().equals(newNickname)
                && memberRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException(
                    MypageErrorCode.MYPAGE_409_NICKNAME_ALREADY_EXISTS.getCode()
            );
        }

        member.updateNickname(newNickname);

        return new MyProfileResponse(
                member.getUserId(),
                member.getEmail(),
                member.getNickname()
        );
    }
}