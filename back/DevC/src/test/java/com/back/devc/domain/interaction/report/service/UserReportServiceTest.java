package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserReportService")
class UserReportServiceTest {

    @InjectMocks
    private UserReportService userReportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    private Member reporter;
    private Member author;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        reporter = mock(Member.class);
        author = mock(Member.class);
        post = mock(Post.class);
        comment = mock(Comment.class);

        given(reporter.getUserId()).willReturn(1L);
        given(author.getUserId()).willReturn(2L);
    }

    @Nested
    @DisplayName("reportPost")
    class ReportPost {

        @Test
        @DisplayName("saves a pending post report when the target is valid")
        void reportPost_savesReport() {
            ReportRequestDTO dto = new ReportRequestDTO(10L, "SPAM", "Repeated promotion");
            givenExistingReporter();
            givenExistingPostByAnotherMember(false);
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, TargetType.POST, 10L))
                    .willReturn(false);

            userReportService.reportPost(1L, dto);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            Report savedReport = captor.getValue();
            assertThat(savedReport.reporter).isSameAs(reporter);
            assertThat(savedReport.targetType).isEqualTo(TargetType.POST);
            assertThat(savedReport.targetId).isEqualTo(10L);
            assertThat(savedReport.reasonType).isEqualTo("SPAM");
            assertThat(savedReport.reasonDetail).isEqualTo("Repeated promotion");
        }

        @Test
        @DisplayName("throws when reporter does not exist")
        void reportPost_throwsWhenReporterMissing() {
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            assertReportError(
                    () -> userReportService.reportPost(1L, new ReportRequestDTO(10L, "SPAM", null)),
                    MemberErrorCode.MEMBER_NOT_FOUND
            );
            verifyNoInteractions(postRepository, commentRepository, reportRepository);
        }

        @Test
        @DisplayName("throws when post does not exist")
        void reportPost_throwsWhenPostMissing() {
            givenExistingReporter();
            given(postRepository.findById(10L)).willReturn(Optional.empty());

            assertReportError(
                    () -> userReportService.reportPost(1L, new ReportRequestDTO(10L, "SPAM", null)),
                    ReportErrorCode.REPORT_404_TARGET
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when reporter owns the post")
        void reportPost_throwsWhenReportingOwnPost() {
            givenExistingReporter();
            given(post.getMember()).willReturn(reporter);
            given(postRepository.findById(10L)).willReturn(Optional.of(post));

            assertReportError(
                    () -> userReportService.reportPost(1L, new ReportRequestDTO(10L, "SPAM", null)),
                    ReportErrorCode.REPORT_400_REPORT_SELF
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when post is already deleted")
        void reportPost_throwsWhenPostDeleted() {
            givenExistingReporter();
            givenExistingPostByAnotherMember(true);

            assertReportError(
                    () -> userReportService.reportPost(1L, new ReportRequestDTO(10L, "SPAM", null)),
                    ReportErrorCode.REPORT_410_ALREADY_DELETED
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when reporter already reported the same post")
        void reportPost_throwsWhenDuplicate() {
            givenExistingReporter();
            givenExistingPostByAnotherMember(false);
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, TargetType.POST, 10L))
                    .willReturn(true);

            assertReportError(
                    () -> userReportService.reportPost(1L, new ReportRequestDTO(10L, "SPAM", null)),
                    ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            );
            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reportComment")
    class ReportComment {

        @Test
        @DisplayName("saves a pending comment report when the target is valid")
        void reportComment_savesReport() {
            ReportRequestDTO dto = new ReportRequestDTO(20L, "ABUSE", "Insulting content");
            givenExistingReporter();
            givenExistingCommentByAnotherMember(false);
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, TargetType.COMMENT, 20L))
                    .willReturn(false);

            userReportService.reportComment(1L, dto);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            Report savedReport = captor.getValue();
            assertThat(savedReport.reporter).isSameAs(reporter);
            assertThat(savedReport.targetType).isEqualTo(TargetType.COMMENT);
            assertThat(savedReport.targetId).isEqualTo(20L);
            assertThat(savedReport.reasonType).isEqualTo("ABUSE");
            assertThat(savedReport.reasonDetail).isEqualTo("Insulting content");
        }

        @Test
        @DisplayName("throws when comment does not exist")
        void reportComment_throwsWhenCommentMissing() {
            givenExistingReporter();
            given(commentRepository.findById(20L)).willReturn(Optional.empty());

            assertReportError(
                    () -> userReportService.reportComment(1L, new ReportRequestDTO(20L, "ABUSE", null)),
                    ReportErrorCode.REPORT_404_TARGET
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when reporter owns the comment")
        void reportComment_throwsWhenReportingOwnComment() {
            givenExistingReporter();
            given(comment.getUserId()).willReturn(1L);
            given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

            assertReportError(
                    () -> userReportService.reportComment(1L, new ReportRequestDTO(20L, "ABUSE", null)),
                    ReportErrorCode.REPORT_400_REPORT_SELF
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when comment is already deleted")
        void reportComment_throwsWhenCommentDeleted() {
            givenExistingReporter();
            givenExistingCommentByAnotherMember(true);

            assertReportError(
                    () -> userReportService.reportComment(1L, new ReportRequestDTO(20L, "ABUSE", null)),
                    ReportErrorCode.REPORT_410_ALREADY_DELETED
            );
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when reporter already reported the same comment")
        void reportComment_throwsWhenDuplicate() {
            givenExistingReporter();
            givenExistingCommentByAnotherMember(false);
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, TargetType.COMMENT, 20L))
                    .willReturn(true);

            assertReportError(
                    () -> userReportService.reportComment(1L, new ReportRequestDTO(20L, "ABUSE", null)),
                    ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            );
            verify(reportRepository, never()).save(any());
        }
    }

    private void givenExistingReporter() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
    }

    private void givenExistingPostByAnotherMember(boolean deleted) {
        given(post.getMember()).willReturn(author);
        given(post.isDeleted()).willReturn(deleted);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
    }

    private void givenExistingCommentByAnotherMember(boolean deleted) {
        given(comment.getUserId()).willReturn(2L);
        given(comment.isDeleted()).willReturn(deleted);
        given(commentRepository.findById(20L)).willReturn(Optional.of(comment));
    }

    private void assertReportError(Runnable action, Object expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(expectedErrorCode);
    }
}
