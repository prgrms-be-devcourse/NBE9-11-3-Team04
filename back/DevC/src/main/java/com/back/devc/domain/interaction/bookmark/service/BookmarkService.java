package com.back.devc.domain.interaction.bookmark.service;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.bookmark.entity.Bookmark;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.util.MemberDisplayUtil;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.errorcode.BookmarkErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    /**
     * 게시글 북마크 추가
     *
     * 동시성 처리 방식:
     * - exists 확인 후 save 하지 않음
     * - DB unique constraint + insert ignore 사용
     * - 이미 북마크된 상태면 그대로 성공 응답
     */
    @Transactional
    public BookmarkResponse createBookmark(BookmarkCreateCommand command) {
        Long userId = command.memberId();
        Long postId = command.postId();

        validateMemberExists(userId);
        validatePostExists(postId);

        bookmarkRepository.insertIgnore(userId, postId);

        return new BookmarkResponse(
                postId,
                true
        );
    }

    /**
     * 게시글 북마크 취소
     *
     * 동시성 처리 방식:
     * - 북마크 엔티티 조회 후 delete 하지 않음
     * - delete query의 affected row 수를 기준으로 처리
     * - 이미 취소된 상태면 그대로 성공 응답
     */
    @Transactional
    public BookmarkResponse cancelBookmark(BookmarkDeleteCommand command) {
        Long userId = command.memberId();
        Long postId = command.postId();

        validateMemberExists(userId);
        validatePostExists(postId);

        bookmarkRepository.deleteByUserIdAndPostId(userId, postId);

        return new BookmarkResponse(
                postId,
                false
        );
    }

    public List<BookmarkedPostResponse> getBookmarkedPosts(Long userId) {
        Member member = findMemberById(userId);

        List<Bookmark> bookmarks = bookmarkRepository.findAllByMemberAndPost_IsDeletedFalse(member);

        return bookmarks.stream()
                .map(bookmark -> {
                    Post post = bookmark.getPost();

                    return new BookmarkedPostResponse(
                            post.getPostId(),
                            post.getTitle(),
                            MemberDisplayUtil.getDisplayName(post.getMember()),
                            post.getCategory().getCategoryId(),
                            post.getLikeCount(),
                            post.getCommentCount(),
                            post.getViewCount(),
                            post.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * 현재 로그인한 사용자가 특정 게시글을 북마크했는지 확인
     */
    public boolean isBookmarkedByUser(Long userId, Long postId) {
        return bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId);
    }

    /**
     * 북마크 목록 조회처럼 Member 엔티티가 필요한 경우 사용
     */
    private Member findMemberById(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getCode()
                ));
    }

    /**
     * 북마크 생성/취소에서는 엔티티 조회 대신 존재 여부만 검증한다.
     */
    private void validateMemberExists(Long userId) {
        if (!memberRepository.existsById(userId)) {
            throw new EntityNotFoundException(
                    BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getCode()
            );
        }
    }

    /**
     * 북마크 생성/취소 대상 게시글 존재 여부 검증
     */
    private void validatePostExists(Long postId) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty()) {
            throw new EntityNotFoundException(
                    BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.getCode()
            );
        }
    }
}