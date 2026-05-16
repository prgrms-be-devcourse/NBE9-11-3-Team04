package com.back.devc.domain.post.comment.service;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService;
import com.back.devc.domain.post.comment.dto.*;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.domain.post.post.service.PostService;
import com.back.devc.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private PostService postService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글 작성 성공 시 댓글을 저장하고 댓글 수 증가 및 댓글 생성 이벤트를 발행한다")
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
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        // when
        CommentResponse response = commentService.createComment(postId, loginUserId, requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCommentId()).isEqualTo(1L);
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getUserId()).isEqualTo(loginUserId);
        assertThat(response.getParentCommentId()).isNull();
        assertThat(response.getContent()).isEqualTo("첫 댓글입니다.");
        // 댓글 작성 성공 시 게시글 댓글 수 증가 로직이 호출되는지 확인
        verify(postService).increaseCommentCount(postId);

        // 댓글 작성과 알림 생성을 분리했으므로 알림 서비스 직접 호출 대신 댓글 생성 이벤트 발행 여부를 검증
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getClass().getSimpleName()).isEqualTo("CommentCreatedEvent");
    }


    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 댓글 생성 이벤트가 발행되지 않는다")
    void createComment_fail_whenPostNotFound_doesNotPublishEvent() {
        // given
        Long loginUserId = 2L;
        Long postId = 999L;
        CommentCreateRequest requestDto = new CommentCreateRequest("존재하지 않는 게시글 댓글");

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class,
                () -> commentService.createComment(postId, loginUserId, requestDto));
        verify(commentRepository, never()).save(any(Comment.class));
        verify(postService, never()).increaseCommentCount(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
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
        ReflectionTestUtils.setField(parentComment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(parentComment, "updatedAt", LocalDateTime.now());

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
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        // when
        CommentResponse response = commentService.createReply(parentCommentId, loginUserId, requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCommentId()).isEqualTo(200L);
        assertThat(response.getParentCommentId()).isEqualTo(parentCommentId);
        assertThat(response.getContent()).isEqualTo("대댓글입니다.");
        // 대댓글 작성 성공 시 부모 댓글이 속한 게시글의 댓글 수 증가 로직이 호출되는지 확인
        verify(postService).increaseCommentCount(postId);

        // 대댓글 작성과 알림 생성을 분리했으므로 알림 서비스 직접 호출 대신 대댓글 생성 이벤트 발행 여부를 검증
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getClass().getSimpleName()).isEqualTo("ReplyCreatedEvent");
    }


    @Test
    @DisplayName("삭제된 댓글에는 답글을 작성할 수 없다")
    void createReply_fail_whenParentDeleted() {
        // given
        Long parentCommentId = 100L;
        Comment deletedParentComment = new Comment(10L, 1L, null, "삭제 전 댓글");
        deletedParentComment.softDelete();
        ReflectionTestUtils.setField(deletedParentComment, "id", parentCommentId);
        ReflectionTestUtils.setField(deletedParentComment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(deletedParentComment, "updatedAt", LocalDateTime.now());

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(deletedParentComment));

        // when & then
        // 현재 서비스는 삭제된 부모 댓글에 답글 작성 시 공통 예외 ApiException을 발생시킴
        assertThrows(ApiException.class,
                () -> commentService.createReply(parentCommentId, 2L, new CommentCreateRequest("대댓글")));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(commentRepository, never()).save(any(Comment.class));
        verify(postService, never()).increaseCommentCount(any());
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
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());

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
        assertThat(response.getContent()).isEqualTo("수정된 댓글");
        assertThat(response.getCommentId()).isEqualTo(commentId);
    }

    @Test
    @DisplayName("댓글을 삭제할 수 있다")
    void deleteComment_success() {
        // given
        Long commentId = 1L;
        Long loginUserId = 2L;

        Comment comment = new Comment(10L, loginUserId, null, "삭제할 댓글");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // when
        CommentDeleteResponse response = commentService.deleteComment(commentId, loginUserId);

        // then
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(response.getCommentId()).isEqualTo(commentId);
        assertThat(response.getMessage()).isEqualTo("댓글 삭제 성공");
        // 댓글 삭제 성공 시 게시글 댓글 수 감소 로직이 호출되는지 확인
        verify(postService).decreaseCommentCount(comment.getPostId());
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
        ReflectionTestUtils.setField(parent, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(parent, "updatedAt", LocalDateTime.now());

        Member parentWriter = mock(Member.class);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(parentWriter));
        when(parentWriter.getNickname()).thenReturn("작성자A");

        when(commentRepository.findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(eq(postId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(parent), PageRequest.of(0, 20), 1));
        when(commentAttachmentService.getAttachments(1L)).thenReturn(new CommentAttachmentListResponse(List.of()));

        // when
        CommentListResponse response = commentService.getComments(postId, 0, 20);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getComments().get(0).getCommentId()).isEqualTo(1L);
        assertThat(response.getComments().get(0).getReplies()).isEmpty();
    }
}
