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

    @Transactional
    public BookmarkResponse createBookmark(BookmarkCreateCommand command) {
        Member member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getCode()
                ));

        Post post = postRepository.findByPostIdAndIsDeletedFalse(command.postId())
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.getCode()
                ));

        if (bookmarkRepository.existsByMemberAndPost(member, post)) {
            return new BookmarkResponse(
                    post.getPostId(),
                    true
            );
        }

        Bookmark bookmark = Bookmark.create(member, post);
        bookmarkRepository.save(bookmark);

        return new BookmarkResponse(
                post.getPostId(),
                true
        );
    }

    @Transactional
    public BookmarkResponse cancelBookmark(BookmarkDeleteCommand command) {
        Member member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getCode()
                ));

        Post post = postRepository.findByPostIdAndIsDeletedFalse(command.postId())
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.getCode()
                ));

        Bookmark bookmark = bookmarkRepository.findByMemberAndPost(member, post)
                .orElse(null);

        if (bookmark == null) {
            return new BookmarkResponse(
                    post.getPostId(),
                    false
            );
        }

        bookmarkRepository.delete(bookmark);

        return new BookmarkResponse(
                post.getPostId(),
                false
        );
    }

    public List<BookmarkedPostResponse> getBookmarkedPosts(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getCode()
                ));

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
}