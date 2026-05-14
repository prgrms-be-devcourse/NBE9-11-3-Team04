package com.back.devc.domain.post.comment.service;

import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.dto.CommentCreateRequest;
import com.back.devc.domain.post.comment.dto.CommentDeleteResponse;
import com.back.devc.domain.post.comment.dto.CommentListResponse;
import com.back.devc.domain.post.comment.dto.CommentResponse;
import com.back.devc.domain.post.comment.dto.CommentUpdateRequest;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentAttachmentService commentAttachmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글을 작성할 수 있다")
    void createComment_success() {
        // given
        Long loginUserId = 2L;
        Long postId = 10L;
        CommentCreateRequest requestDto = new CommentCreateRequest("첫 댓글입니다.");

        Post post = mock(Post.class);
        Member member = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getTitle()).thenReturn("테스트 게시글");
        when(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member));
        when(member.getUserId()).thenReturn(loginUserId);
        when(member.getNickname()).thenReturn("작성자B");
        when(commentAttachmentService.getAttachments(1L)).thenReturn(new CommentAttachmentListResponse(List.of()));

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        CommentResponse response = commentService.createComment(postId, loginUserId, requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.userId()).isEqualTo(loginUserId);
        assertThat(response.parentCommentId()).isNull();
        assertThat(response.content()).isEqualTo("첫 댓글입니다.");
        verify(notificationService).createCommentNotification(postId, loginUserId, 1L);
    }

    @Test
    @DisplayName("내 게시글에 내가 댓글을 작성해도 서비스는 댓글 저장 후 알림 생성 메서드를 호출한다")
    void createComment_callsNotificationMethod_evenWhenSelfComment() {
        // given
        Long loginUserId = 2L;
        Long postId = 10L;
        CommentCreateRequest requestDto = new CommentCreateRequest("내 글의 댓글입니다.");

        Post post = mock(Post.class);
        Member member = mock(Member.class);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getTitle()).thenReturn("내 게시글");
        when(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member));
        when(member.getUserId()).thenReturn(loginUserId);
        when(member.getNickname()).thenReturn("작성자A");
        when(commentAttachmentService.getAttachments(2L)).thenReturn(new CommentAttachmentListResponse(List.of()));

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        // when
        CommentResponse response = commentService.createComment(postId, loginUserId, requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.commentId()).isEqualTo(2L);
        verify(notificationService).createCommentNotification(postId, loginUserId, 2L);
    }

    @Test
    @DisplayName("대댓글을 작성할 수 있다")
    void createReply_success() {
        // given
        Long loginUserId = 2L;
        Long parentCommentId = 100L;
        Long postId = 10L;
        CommentCreateRequest requestDto = new CommentCreateRequest("대댓글입니다.");

        Comment parentComment = new Comment(postId, 1L, null, "부모 댓글");
        ReflectionTestUtils.setField(parentComment, "id", parentCommentId);

        Post post = mock(Post.class);
        Member member = mock(Member.class);

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getTitle()).thenReturn("테스트 게시글");
        when(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member));
        when(member.getUserId()).thenReturn(loginUserId);
        when(member.getNickname()).thenReturn("작성자B");
        when(commentAttachmentService.getAttachments(200L)).thenReturn(new CommentAttachmentListResponse(List.of()));

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 200L);
            return saved;
        });

        // when
        CommentResponse response = commentService.createReply(parentCommentId, loginUserId, requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.commentId()).isEqualTo(200L);
        assertThat(response.parentCommentId()).isEqualTo(parentCommentId);
        assertThat(response.content()).isEqualTo("대댓글입니다.");
        verify(notificationService).createReplyNotification(parentCommentId, loginUserId, 200L);
    }

    @Test
    @DisplayName("삭제된 댓글에는 답글을 작성할 수 없다")
    void createReply_fail_whenParentDeleted() {
        // given
        Long parentCommentId = 100L;
        Comment deletedParentComment = new Comment(10L, 1L, null, "삭제 전 댓글");
        deletedParentComment.softDelete();

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(deletedParentComment));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> commentService.createReply(parentCommentId, 2L, new CommentCreateRequest("대댓글")));
        verify(notificationService, never()).createReplyNotification(any(), any(), any());
    }

    @Test
    @DisplayName("댓글을 수정할 수 있다")
    void updateComment_success() {
        // given
        Long commentId = 1L;
        Long loginUserId = 2L;
        Long postId = 10L;
        CommentUpdateRequest requestDto = new CommentUpdateRequest("수정된 댓글");

        Comment comment = new Comment(postId, loginUserId, null, "기존 댓글");
        ReflectionTestUtils.setField(comment, "id", commentId);

        Post post = mock(Post.class);
        Member member = mock(Member.class);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getTitle()).thenReturn("테스트 게시글");
        when(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member));
        when(member.getNickname()).thenReturn("작성자B");
        when(commentAttachmentService.getAttachments(commentId)).thenReturn(new CommentAttachmentListResponse(List.of()));

        // when
        CommentResponse response = commentService.updateComment(commentId, loginUserId, requestDto);

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 댓글");
        assertThat(response.content()).isEqualTo("수정된 댓글");
        assertThat(response.commentId()).isEqualTo(commentId);
    }

    @Test
    @DisplayName("댓글을 삭제할 수 있다")
    void deleteComment_success() {
        // given
        Long commentId = 1L;
        Long loginUserId = 2L;

        Comment comment = new Comment(10L, loginUserId, null, "삭제할 댓글");
        ReflectionTestUtils.setField(comment, "id", commentId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // when
        CommentDeleteResponse response = commentService.deleteComment(commentId, loginUserId);

        // then
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(response.commentId()).isEqualTo(commentId);
        assertThat(response.message()).isEqualTo("댓글 삭제 성공");
    }

    @Test
    @DisplayName("게시글의 댓글 목록을 조회할 수 있다")
    void getComments_success() {
        // given
        Long postId = 10L;
        Post post = mock(Post.class);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getTitle()).thenReturn("테스트 게시글");

        Comment parent = new Comment(postId, 1L, null, "부모 댓글");
        ReflectionTestUtils.setField(parent, "id", 1L);

        Comment reply = new Comment(postId, 2L, 1L, "대댓글");
        ReflectionTestUtils.setField(reply, "id", 2L);

        Member parentWriter = mock(Member.class);
        Member replyWriter = mock(Member.class);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(parentWriter));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(replyWriter));
        when(parentWriter.getNickname()).thenReturn("작성자A");
        when(replyWriter.getNickname()).thenReturn("작성자B");

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(parent, reply));
        when(commentAttachmentService.getAttachments(1L)).thenReturn(new CommentAttachmentListResponse(List.of()));
        when(commentAttachmentService.getAttachments(2L)).thenReturn(new CommentAttachmentListResponse(List.of()));

        // when
        CommentListResponse response = commentService.getComments(postId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).commentId()).isEqualTo(1L);
        assertThat(response.comments().get(0).replies()).hasSize(1);
        assertThat(response.comments().get(0).replies().get(0).commentId()).isEqualTo(2L);
    }
}
