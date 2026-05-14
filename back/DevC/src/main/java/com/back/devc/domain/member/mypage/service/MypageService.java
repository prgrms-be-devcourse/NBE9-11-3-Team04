package com.back.devc.domain.member.mypage.service;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery;
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
import com.back.devc.global.exception.errorcode.MypageErrorCode;
import com.back.devc.global.response.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
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

        Page<MyPostResponse> response = posts.map(post -> new MyPostResponse(
                post.getPostId(),
                post.getTitle(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                post.getCreatedAt(),
                postLikeService.isLikedByUser(userId, post.getPostId()),
                bookmarkService.isBookmarkedByUser(userId, post.getPostId())
        ));

        return PageResponse.from(response);
    }

    public PageResponse<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {
        getMember(userId);

        Page<MyCommentResponse> comments = commentRepository.findMyComments(userId, pageable);

        return PageResponse.from(comments);
    }

    public PageResponse<LikedPostResponse> getMyLikedPosts(Long userId, Pageable pageable) {
        getMember(userId);

        List<LikedPostResponse> likedPosts =
                postLikeService.getLikedPosts(new LikedPostsQuery(userId));

        Page<LikedPostResponse> page = toPage(likedPosts, pageable);

        return PageResponse.from(page);
    }

    public PageResponse<BookmarkedPostResponse> getMyBookmarkedPosts(Long userId, Pageable pageable) {
        getMember(userId);

        List<BookmarkedPostResponse> bookmarkedPosts =
                bookmarkService.getBookmarkedPosts(userId);

        Page<BookmarkedPostResponse> page = toPage(bookmarkedPosts, pageable);

        return PageResponse.from(page);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        Member member = getMember(userId);

        String newNickname = request.nickname().trim();

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

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();

        if (start >= list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }

        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> content = list.subList(start, end);

        return new PageImpl<>(content, pageable, list.size());
    }
}