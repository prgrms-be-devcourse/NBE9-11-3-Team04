package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.util.ReportTargetHandler;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.service.MemberSanctionService;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportTargetHandlerTest {

    @Mock
    PostRepository postRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    MemberRepository memberRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    MemberSanctionService memberSanctionService;

    @InjectMocks
    ReportTargetHandler handler;



//    ReportTargetHandlerTest → “실제 비즈니스 로직” 테스트
//    검증 대상:
//    POST 삭제 (delete)
//    COMMENT 삭제 (softDelete)
//    notification 생성
//    member sanction 적용
//    suspensionDays 반영
//    member resolve 로직
//    예외 (target/member 없음)

    // 1. handleApproved - POST (sanction 없음)
    @Test
    void handleApproved_post_noSanction() {
        // 1. Given: 필요한 객체들만 모킹
        Member admin = mock(Member.class);
        when(admin.getUserId()).thenReturn(1L);

        Post post = mock(Post.class);
        // post.getMember() 호출이 실제 로직에서 발생하지 않는다면 writer 모킹은 필요 없습니다.
        when(post.isDeleted()).thenReturn(false);

        when(postRepository.findById(10L))
                .thenReturn(Optional.of(post));

        // 2. When: 테스트 대상 메서드 실행
        handler.handleApproved(
                TargetType.POST,
                10L,
                admin,
                null, // sanctionType이 null이므로 memberSanctionService는 호출되지 않아야 함
                null
        );

        // 3. Then: 행위 검증
        // 알림 서비스가 호출되었는지 확인
        verify(notificationService)
                .createPostReportNotification(10L, 1L);

        // 리포지토리 조회 및 게시글 삭제(soft delete 등) 호출 확인
        verify(postRepository).findById(10L);
        verify(post).delete();

        // 제재 서비스는 호출되지 않았음을 확신
        verifyNoInteractions(memberSanctionService);
    }
    //2. handleApproved - COMMENT + sanction
    @Test
    void handleApproved_comment_withSanction() {

        Member admin = mock(Member.class);
        when(admin.getUserId()).thenReturn(1L);

        Comment comment = mock(Comment.class);
        Member writer = mock(Member.class);

        when(comment.isDeleted()).thenReturn(false);
        when(comment.getUserId()).thenReturn(99L);

        when(commentRepository.findById(20L))
                .thenReturn(Optional.of(comment));

        when(memberRepository.findById(99L))
                .thenReturn(Optional.of(writer));

        handler.handleApproved(
                TargetType.COMMENT,
                20L,
                admin,
                SanctionType.WARNED,
                0
        );

        verify(notificationService)
                .createCommentReportNotification(20L, 1L);

        verify(comment).softDelete();

        verify(memberSanctionService)
                .apply(writer, MemberStatus.WARNED, 0);
    }

    // 3. deleteTarget - POST already deleted 상태
    @Test
    void deleteTarget_post_alreadyDeleted() {

        Post post = mock(Post.class);
        when(post.isDeleted()).thenReturn(true);

        when(postRepository.findById(10L))
                .thenReturn(Optional.of(post));

        handler.handleApproved(
                TargetType.POST,
                10L,
                mock(Member.class),
                null,
                null
        );

        verify(post, never()).delete();
    }

    // 4. COMMENT 삭제 케이스
    @Test
    void deleteTarget_comment_success() {

        Comment comment = mock(Comment.class);
        when(comment.isDeleted()).thenReturn(false);

        when(commentRepository.findById(20L))
                .thenReturn(Optional.of(comment));

        handler.handleApproved(
                TargetType.COMMENT,
                20L,
                mock(Member.class),
                null,
                null
        );

        verify(comment).softDelete();
    }

    // 5. applySanction - SUSPENDED
    @Test
    void applySanction_suspended() {

        Member writer = mock(Member.class);

        Post post = mock(Post.class);
        when(post.getMember()).thenReturn(writer);

        when(postRepository.findById(10L))
                .thenReturn(Optional.of(post));

        Member admin = mock(Member.class);
        when(admin.getUserId()).thenReturn(1L);

        handler.handleApproved(
                TargetType.POST,
                10L,
                admin,
                SanctionType.SUSPENDED,
                7
        );

        verify(memberSanctionService)
                .apply(writer, MemberStatus.SUSPENDED, 7);
    }

    // 6. findTargetMember - POST 대상이 존재하지 않는 경우
    @Test
    void findTargetMember_post_notFound() {

        when(postRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                handler.handleApproved(
                        TargetType.POST,
                        10L,
                        mock(Member.class),
                        SanctionType.WARNED,
                        null
                )
        ).isInstanceOf(ApiException.class);
    }

    // 7. COMMENT → member 없을 때 예외
    @Test
    void findTargetMember_comment_memberNotFound() {

        Comment comment = mock(Comment.class);
        when(comment.getUserId()).thenReturn(99L);

        when(commentRepository.findById(20L))
                .thenReturn(Optional.of(comment));

        when(memberRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                handler.handleApproved(
                        TargetType.COMMENT,
                        20L,
                        mock(Member.class),
                        SanctionType.WARNED,
                        null
                )
        ).isInstanceOf(ApiException.class);
    }

    // 8. handleRejected
    @Test
    void handleRejected_success() {

        Member admin = mock(Member.class);
        when(admin.getUserId()).thenReturn(1L);

        handler.handleRejected(
                TargetType.POST,
                10L,
                admin
        );

        verify(notificationService)
                .createPostReportNotification(10L, 1L);
    }

    // 10. exists 테스트
    @Test
    void exists_post_true() {

        when(postRepository.existsById(10L))
                .thenReturn(true);

        boolean result = handler.exists(TargetType.POST, 10L);

        assertThat(result).isTrue();
    }
}