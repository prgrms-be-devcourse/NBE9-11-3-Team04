package com.back.devc.domain.interaction.bookmark.service;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.bookmark.entity.Bookmark;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.BookmarkErrorCode;
import com.back.devc.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@DisplayName("BookmarkService 테스트")
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @InjectMocks
    private BookmarkService bookmarkService;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    private final Long userId = 1L;
    private final Long postId = 10L;

    @Test
    @DisplayName("북마크 추가 성공")
    void createBookmark_success() {
        // given
        BookmarkCreateCommand command = new BookmarkCreateCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(bookmarkRepository.insertIgnore(userId, postId))
                .willReturn(1);

        // when
        BookmarkResponse response = bookmarkService.createBookmark(command);

        // then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getBookmarked()).isTrue();

        then(memberRepository).should().existsById(userId);
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId);
        then(bookmarkRepository).should().insertIgnore(userId, postId);
    }

    @Test
    @DisplayName("이미 북마크된 게시글이어도 성공 응답을 반환한다")
    void createBookmark_alreadyExists() {
        // given
        BookmarkCreateCommand command = new BookmarkCreateCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(bookmarkRepository.insertIgnore(userId, postId))
                .willReturn(0);

        // when
        BookmarkResponse response = bookmarkService.createBookmark(command);

        // then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getBookmarked()).isTrue();

        then(bookmarkRepository).should().insertIgnore(userId, postId);
    }

    @Test
    @DisplayName("북마크 추가 시 회원이 없으면 예외가 발생한다")
    void createBookmark_memberNotFound() {
        // given
        BookmarkCreateCommand command = new BookmarkCreateCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> bookmarkService.createBookmark(command))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getMessage());

        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong());
        then(bookmarkRepository).should(never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 추가 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    void createBookmark_postNotFound() {
        // given
        BookmarkCreateCommand command = new BookmarkCreateCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.createBookmark(command))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.getMessage());

        then(bookmarkRepository).should(never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 취소 성공")
    void cancelBookmark_success() {
        // given
        BookmarkDeleteCommand command = new BookmarkDeleteCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(bookmarkRepository.deleteByUserIdAndPostId(userId, postId))
                .willReturn(1);

        // when
        BookmarkResponse response = bookmarkService.cancelBookmark(command);

        // then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getBookmarked()).isFalse();

        then(memberRepository).should().existsById(userId);
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId);
        then(bookmarkRepository).should().deleteByUserIdAndPostId(userId, postId);
    }

    @Test
    @DisplayName("이미 취소된 북마크여도 성공 응답을 반환한다")
    void cancelBookmark_alreadyCanceled() {
        // given
        BookmarkDeleteCommand command = new BookmarkDeleteCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(bookmarkRepository.deleteByUserIdAndPostId(userId, postId))
                .willReturn(0);

        // when
        BookmarkResponse response = bookmarkService.cancelBookmark(command);

        // then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getBookmarked()).isFalse();

        then(bookmarkRepository).should().deleteByUserIdAndPostId(userId, postId);
    }

    @Test
    @DisplayName("북마크 취소 시 회원이 없으면 예외가 발생한다")
    void cancelBookmark_memberNotFound() {
        // given
        BookmarkDeleteCommand command = new BookmarkDeleteCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> bookmarkService.cancelBookmark(command))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getMessage());

        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong());
        then(bookmarkRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 취소 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    void cancelBookmark_postNotFound() {
        // given
        BookmarkDeleteCommand command = new BookmarkDeleteCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.cancelBookmark(command))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.getMessage());

        then(bookmarkRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내가 북마크한 게시글 목록을 반환한다")
    void getBookmarkedPosts_success() {
        // given
        Member member = createMemberWithId(userId, "작성자");

        Category category = mock(Category.class);
        given(category.getCategoryId()).willReturn(100L);

        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        Post post = createPostWithId(
                postId,
                member,
                category,
                "테스트 제목",
                7,
                2,
                100,
                createdAt
        );

        Bookmark bookmark = Bookmark.create(member, post);

        Pageable pageable = PageRequest.of(0, 10);

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));

        given(bookmarkRepository.findAllByMemberAndPost_IsDeletedFalse(
                member,
                pageable
        )).willReturn(new PageImpl<>(List.of(bookmark), pageable, 1));

        given(postLikeRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
                .willReturn(true);

        // when
        PageResponse<BookmarkedPostResponse> response =
                bookmarkService.getBookmarkedPosts(userId, pageable);

        // then
        assertThat(response.content()).hasSize(1);

        BookmarkedPostResponse bookmarkedPost = response.content().get(0);

        assertThat(bookmarkedPost.getPostId()).isEqualTo(postId);
        assertThat(bookmarkedPost.getTitle()).isEqualTo("테스트 제목");
        assertThat(bookmarkedPost.getAuthorNickname()).isEqualTo("작성자");
        assertThat(bookmarkedPost.getCategoryId()).isEqualTo(100L);
        assertThat(bookmarkedPost.getLikeCount()).isEqualTo(7L);
        assertThat(bookmarkedPost.getCommentCount()).isEqualTo(2L);
        assertThat(bookmarkedPost.getViewCount()).isEqualTo(100L);
        assertThat(bookmarkedPost.getCreatedAt()).isEqualTo(createdAt);
        assertThat(bookmarkedPost.getLiked()).isTrue();
        assertThat(bookmarkedPost.getBookmarked()).isTrue();

        then(memberRepository).should().findById(userId);

        then(bookmarkRepository).should()
                .findAllByMemberAndPost_IsDeletedFalse(
                        member,
                        pageable
                );

        then(postLikeRepository).should()
                .existsByMember_UserIdAndPost_PostId(userId, postId);
    }

    @Test
    @DisplayName("북마크 목록 조회 시 회원이 없으면 예외가 발생한다")
    void getBookmarkedPosts_memberNotFound() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        given(memberRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.getBookmarkedPosts(userId, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.getMessage());

        then(bookmarkRepository).should(never())
                .findAllByMemberAndPost_IsDeletedFalse(
                        any(Member.class),
                        any(Pageable.class)
                );

        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("사용자가 특정 게시글을 북마크했는지 확인한다")
    void isBookmarkedByUser_success() {
        // given
        given(bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
                .willReturn(true);

        // when
        boolean result = bookmarkService.isBookmarkedByUser(userId, postId);

        // then
        assertThat(result).isTrue();

        then(bookmarkRepository).should()
                .existsByMember_UserIdAndPost_PostId(userId, postId);
    }

    private Member createMemberWithId(Long userId, String nickname) {
        Member member = Member.createLocalMember(
                "test@test.com",
                "encodedPassword",
                nickname
        );

        setField(member, "userId", userId);

        return member;
    }

    private Post createPostWithId(
            Long postId,
            Member member,
            Category category,
            String title,
            int likeCount,
            int commentCount,
            int viewCount,
            LocalDateTime createdAt
    ) {
        Post post = Post.create(
                member,
                category,
                title,
                "테스트 내용"
        );

        setField(post, "postId", postId);
        setField(post, "likeCount", likeCount);
        setField(post, "commentCount", commentCount);
        setField(post, "viewCount", viewCount);
        setField(post, "isDeleted", false);
        setField(post, "createdAt", createdAt);
        setField(post, "updatedAt", createdAt);

        return post;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}