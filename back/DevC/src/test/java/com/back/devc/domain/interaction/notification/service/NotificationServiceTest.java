package com.back.devc.domain.interaction.notification.service;

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse;
import com.back.devc.domain.interaction.notification.dto.NotificationResponse;
import com.back.devc.domain.interaction.notification.entity.Notification;
import com.back.devc.domain.interaction.notification.repository.NotificationRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("내 알림 목록을 조회할 수 있다")
    void getMyNotifications_success() {
        // given
        Long loginUserId = 1L;
        Long actorUserId = 2L;

        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(10L);
        when(notification.getUserId()).thenReturn(loginUserId);
        when(notification.getActorUserId()).thenReturn(actorUserId);
        when(notification.getPostId()).thenReturn(100L);
        when(notification.getCommentId()).thenReturn(200L);
        when(notification.getType()).thenReturn("COMMENT");
        when(notification.getMessage()).thenReturn("댓글 알림 메시지");
        when(notification.isRead()).thenReturn(false);
        when(notification.getCreatedAt()).thenReturn(LocalDateTime.now());

        Member actor = mock(Member.class);
        when(actor.getNickname()).thenReturn("작성자B");

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(loginUserId)).thenReturn(List.of(notification));
        when(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor));

        // when
        NotificationListResponse response = notificationService.getMyNotifications(loginUserId);

        // then
        assertNotNull(response);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(loginUserId);
        verify(memberRepository).findById(actorUserId);
    }

    @Test
    @DisplayName("본인 알림은 읽음 처리할 수 있다")
    void readNotification_success() {
        // given
        Long notificationId = 1L;
        Long loginUserId = 10L;
        Long actorUserId = 20L;
        AtomicBoolean read = new AtomicBoolean(false);

        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(notificationId);
        when(notification.getUserId()).thenReturn(loginUserId);
        when(notification.getActorUserId()).thenReturn(actorUserId);
        when(notification.getPostId()).thenReturn(100L);
        when(notification.getCommentId()).thenReturn(200L);
        when(notification.getType()).thenReturn("COMMENT");
        when(notification.getMessage()).thenReturn("댓글 알림 메시지");
        when(notification.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(notification.isRead()).thenAnswer(invocation -> read.get());
        doAnswer(invocation -> {
            read.set(true);
            return null;
        }).when(notification).markAsRead();

        Member actor = mock(Member.class);
        when(actor.getNickname()).thenReturn("작성자B");

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor));

        // when
        NotificationResponse response = notificationService.readNotification(notificationId, loginUserId);

        // then
        assertNotNull(response);
        assertThat(read.get()).isTrue();
        verify(notification).markAsRead();
    }

    @Test
    @DisplayName("다른 사람 알림은 읽음 처리할 수 없다")
    void readNotification_fail_whenOtherUsersNotification() {
        // given
        Long notificationId = 1L;
        Long loginUserId = 10L;

        Notification notification = mock(Notification.class);
        when(notification.getUserId()).thenReturn(999L);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.readNotification(notificationId, loginUserId));
    }

    @Test
    @DisplayName("다른 사용자가 내 게시글에 댓글을 작성하면 댓글 알림이 생성된다")
    void createCommentNotification_success() {
        // given
        Long postId = 100L;
        Long actorUserId = 2L;
        Long commentId = 300L;
        Long postOwnerId = 1L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);
        Member actor = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(postOwnerId);
        when(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor));
        when(actor.getNickname()).thenReturn("작성자B");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.createCommentNotification(postId, actorUserId, commentId);

        // then
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(postOwnerId);
        assertThat(saved.getActorUserId()).isEqualTo(actorUserId);
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getCommentId()).isEqualTo(commentId);
        assertThat(saved.getType()).isEqualTo("COMMENT");
        assertThat(saved.getMessage()).contains("게시글에 댓글을 남겼습니다");
    }

    @Test
    @DisplayName("내가 내 게시글에 댓글을 작성하면 댓글 알림은 생성되지 않는다")
    void createCommentNotification_noNotification_whenSelfComment() {
        // given
        Long postId = 100L;
        Long actorUserId = 1L;
        Long commentId = 300L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(actorUserId);

        // when
        notificationService.createCommentNotification(postId, actorUserId, commentId);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("다른 사용자가 내 댓글에 답글을 작성하면 답글 알림이 생성된다")
    void createReplyNotification_success() {
        // given
        Long parentCommentId = 10L;
        Long actorUserId = 2L;
        Long replyCommentId = 20L;
        Long receiverUserId = 1L;

        Comment parentComment = mock(Comment.class);
        Member actor = mock(Member.class);

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(parentComment.isDeleted()).thenReturn(false);
        when(parentComment.getUserId()).thenReturn(receiverUserId);
        when(parentComment.getPostId()).thenReturn(100L);
        when(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor));
        when(actor.getNickname()).thenReturn("작성자B");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.createReplyNotification(parentCommentId, actorUserId, replyCommentId);

        // then
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(receiverUserId);
        assertThat(saved.getActorUserId()).isEqualTo(actorUserId);
        assertThat(saved.getPostId()).isEqualTo(100L);
        assertThat(saved.getCommentId()).isEqualTo(replyCommentId);
        assertThat(saved.getType()).isEqualTo("REPLY");
        assertThat(saved.getMessage()).contains("답글을 남겼습니다");
    }

    @Test
    @DisplayName("삭제된 부모 댓글에는 답글 알림이 생성되지 않는다")
    void createReplyNotification_noNotification_whenParentCommentDeleted() {
        // given
        Long parentCommentId = 10L;

        Comment parentComment = mock(Comment.class);
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(parentComment.isDeleted()).thenReturn(true);

        // when
        notificationService.createReplyNotification(parentCommentId, 2L, 20L);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("다른 사용자가 내 게시글에 좋아요를 누르면 좋아요 알림이 생성된다")
    void createPostLikeNotification_success() {
        // given
        Long postId = 100L;
        Long actorUserId = 2L;
        Long postOwnerId = 1L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);
        Member actor = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(postOwnerId);
        when(notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(postOwnerId, actorUserId, postId, "LIKE"))
                .thenReturn(false);
        when(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor));
        when(actor.getNickname()).thenReturn("작성자B");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.createPostLikeNotification(postId, actorUserId);

        // then
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(postOwnerId);
        assertThat(saved.getActorUserId()).isEqualTo(actorUserId);
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getType()).isEqualTo("LIKE");
        assertThat(saved.getMessage()).contains("좋아합니다");
    }

    @Test
    @DisplayName("내가 내 게시글에 좋아요를 누르면 좋아요 알림은 생성되지 않는다")
    void createPostLikeNotification_noNotification_whenSelfLike() {
        // given
        Long postId = 100L;
        Long actorUserId = 1L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(actorUserId);

        // when
        notificationService.createPostLikeNotification(postId, actorUserId);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("같은 사용자가 같은 게시글에 다시 좋아요를 눌러도 중복 알림은 생성되지 않는다")
    void createPostLikeNotification_noDuplicateNotification() {
        // given
        Long postId = 100L;
        Long actorUserId = 2L;
        Long postOwnerId = 1L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(postOwnerId);
        when(notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(postOwnerId, actorUserId, postId, "LIKE"))
                .thenReturn(true);

        // when
        notificationService.createPostLikeNotification(postId, actorUserId);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("관리자 처리 후 게시글 신고 알림이 생성된다")
    void createPostReportNotification_success() {
        // given
        Long postId = 100L;
        Long adminUserId = 99L;
        Long postOwnerId = 1L;

        Post post = mock(Post.class);
        Member owner = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(owner);
        when(owner.getUserId()).thenReturn(postOwnerId);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(postOwnerId)).thenReturn(List.of());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.createPostReportNotification(postId, adminUserId);

        // then
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(postOwnerId);
        assertThat(saved.getActorUserId()).isEqualTo(adminUserId);
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getType()).isEqualTo("REPORT");
    }

    @Test
    @DisplayName("관리자 처리 후 댓글 신고 알림이 생성된다")
    void createCommentReportNotification_success() {
        // given
        Long commentId = 200L;
        Long adminUserId = 99L;
        Long commentOwnerId = 1L;

        Comment comment = mock(Comment.class);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(comment.getUserId()).thenReturn(commentOwnerId);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(commentOwnerId)).thenReturn(List.of());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.createCommentReportNotification(commentId, adminUserId);

        // then
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(commentOwnerId);
        assertThat(saved.getActorUserId()).isEqualTo(adminUserId);
        assertThat(saved.getCommentId()).isEqualTo(commentId);
        assertThat(saved.getType()).isEqualTo("REPORT");
    }
}
