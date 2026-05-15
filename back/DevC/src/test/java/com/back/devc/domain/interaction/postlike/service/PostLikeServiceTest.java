package com.back.devc.domain.interaction.postlike.service;

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery;
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand;
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse;
import com.back.devc.domain.interaction.postLike.entity.PostLike;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.errorCode.PostLikeErrorCode;
import com.back.devc.global.response.successCode.PostLikeSuccessCode;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@DisplayName("PostLikeService 테스트")
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private NotificationService notificationService;

    private final Long userId = 1L;
    private final Long postId = 10L;

    @Test
    @DisplayName("처음 좋아요하면 좋아요가 생성되고 카운트가 증가한다")
    void createLike_success() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(postLikeRepository.insertIgnore(userId, postId))
                .willReturn(1);
        given(postRepository.findLikeCountByPostId(postId))
                .willReturn(5);

        // when
        PostLikeResponse response = postLikeService.createLike(command);

        // then
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(5);
        assertThat(response.message())
                .isEqualTo(PostLikeSuccessCode.POST_LIKE_CREATED.getMessage());

        then(postRepository).should().increaseLikeCount(postId);
        then(notificationService).should()
                .createPostLikeNotification(postId, userId);
    }

    @Test
    @DisplayName("이미 좋아요한 게시글이면 카운트와 알림을 생성하지 않는다")
    void createLike_alreadyExists() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(postLikeRepository.insertIgnore(userId, postId))
                .willReturn(0);
        given(postRepository.findLikeCountByPostId(postId))
                .willReturn(3);

        // when
        PostLikeResponse response = postLikeService.createLike(command);

        // then
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(3);
        assertThat(response.message())
                .isEqualTo(PostLikeSuccessCode.POST_LIKE_ALREADY_EXISTS.getMessage());

        then(postRepository).should(never()).increaseLikeCount(anyLong());
        then(notificationService).should(never())
                .createPostLikeNotification(anyLong(), anyLong());
    }

    @Test
    @DisplayName("좋아요 생성 시 회원이 없으면 예외가 발생한다")
    void createLike_memberNotFound() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> postLikeService.createLike(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode());

        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong());
        then(postLikeRepository).should(never()).insertIgnore(anyLong(), anyLong());
        then(postRepository).should(never()).increaseLikeCount(anyLong());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("좋아요 생성 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    void createLike_postNotFound() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.createLike(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.getCode());

        then(postLikeRepository).should(never()).insertIgnore(anyLong(), anyLong());
        then(postRepository).should(never()).increaseLikeCount(anyLong());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("좋아요가 존재하면 삭제되고 카운트가 감소한다")
    void cancelLike_success() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(postLikeRepository.deleteByUserIdAndPostId(userId, postId))
                .willReturn(1);
        given(postRepository.findLikeCountByPostId(postId))
                .willReturn(2);

        // when
        PostLikeResponse response = postLikeService.cancelLike(command);

        // then
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2);
        assertThat(response.message())
                .isEqualTo(PostLikeSuccessCode.POST_LIKE_CANCELED.getMessage());

        then(postRepository).should().decreaseLikeCount(postId);
    }

    @Test
    @DisplayName("이미 취소된 상태면 카운트를 감소하지 않는다")
    void cancelLike_alreadyCanceled() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);
        Post post = mock(Post.class);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));
        given(postLikeRepository.deleteByUserIdAndPostId(userId, postId))
                .willReturn(0);
        given(postRepository.findLikeCountByPostId(postId))
                .willReturn(2);

        // when
        PostLikeResponse response = postLikeService.cancelLike(command);

        // then
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2);
        assertThat(response.message())
                .isEqualTo(PostLikeSuccessCode.POST_LIKE_ALREADY_CANCELED.getMessage());

        then(postRepository).should(never()).decreaseLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요 취소 시 회원이 없으면 예외가 발생한다")
    void cancelLike_memberNotFound() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> postLikeService.cancelLike(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode());

        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong());
        then(postLikeRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong());
        then(postRepository).should(never()).decreaseLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요 취소 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    void cancelLike_postNotFound() {
        // given
        PostLikeCommand command = new PostLikeCommand(userId, postId);

        given(memberRepository.existsById(userId)).willReturn(true);
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.cancelLike(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.getCode());

        then(postLikeRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong());
        then(postRepository).should(never()).decreaseLikeCount(anyLong());
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록을 반환한다")
    void getLikedPosts_success() {
        // given
        Member member = createMemberWithId(userId, "작성자");

        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        Post post = createPostWithId(
                postId,
                member,
                "테스트 제목",
                7,
                2,
                100,
                createdAt
        );

        PostLike postLike = PostLike.create(member, post);
        LikedPostsQuery query = new LikedPostsQuery(userId);

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(postLikeRepository.findAllByMemberAndPost_IsDeletedFalse(member))
                .willReturn(List.of(postLike));
        given(bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
                .willReturn(true);

        // when
        List<LikedPostResponse> responses = postLikeService.getLikedPosts(query);

        // then
        assertThat(responses).hasSize(1);

        LikedPostResponse response = responses.get(0);

        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.title()).isEqualTo("테스트 제목");
        assertThat(response.authorNickname()).isEqualTo("작성자");
        assertThat(response.likeCount()).isEqualTo(7);
        assertThat(response.commentCount()).isEqualTo(2);
        assertThat(response.viewCount()).isEqualTo(100);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.liked()).isTrue();
        assertThat(response.bookmarked()).isTrue();

        then(memberRepository).should().findById(userId);
        then(postLikeRepository).should()
                .findAllByMemberAndPost_IsDeletedFalse(member);
        then(bookmarkRepository).should()
                .existsByMember_UserIdAndPost_PostId(userId, postId);
    }

    @Test
    @DisplayName("좋아요한 게시글 목록 조회 시 회원이 없으면 예외가 발생한다")
    void getLikedPosts_memberNotFound() {
        // given
        LikedPostsQuery query = new LikedPostsQuery(userId);

        given(memberRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.getLikedPosts(query))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.getCode());

        then(postLikeRepository).should(never())
                .findAllByMemberAndPost_IsDeletedFalse(any(Member.class));
        verifyNoInteractions(bookmarkRepository);
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
            String title,
            int likeCount,
            int commentCount,
            int viewCount,
            LocalDateTime createdAt
    ) {
        Post post = Post.builder()
                .member(member)
                .title(title)
                .content("테스트 내용")
                .likeCount(likeCount)
                .commentCount(commentCount)
                .viewCount(viewCount)
                .isDeleted(false)
                .createdAt(createdAt)
                .build();

        setField(post, "postId", postId);

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