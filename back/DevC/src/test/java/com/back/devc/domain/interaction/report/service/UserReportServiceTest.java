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
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    @InjectMocks
    private UserReportService userReportService;

    @Mock private ReportRepository reportRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;

    private Member reporter;
    private Member author;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {

        reporter = mock(Member.class);
        author = mock(Member.class);

        given(reporter.getUserId()).willReturn(1L);
        given(author.getUserId()).willReturn(2L);

        post = mock(Post.class);
        given(post.getMember()).willReturn(author);
        given(post.isDeleted()).willReturn(false);

        comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(2L);
        given(comment.isDeleted()).willReturn(false);
    }

    // =========================================================
    // POST 신고
    // =========================================================
    @Nested
    class PostReport {

        @Test
        void 성공() {

            ReportRequestDTO dto = new ReportRequestDTO(10L, "ABUSE", "x");

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(postRepository.findById(10L)).willReturn(Optional.of(post));
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(
                    reporter, TargetType.POST, 10L
            )).willReturn(false);

            userReportService.reportPost(1L, dto);

            verify(reportRepository).save(any(Report.class));
        }

        @Test
        void 신고자없음() {

            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userReportService.reportPost(1L,
                            new ReportRequestDTO(10L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        void 게시글없음() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(postRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userReportService.reportPost(1L,
                            new ReportRequestDTO(10L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_404_TARGET);
        }

        @Test
        void 본인게시글() {

            given(post.getMember()).willReturn(reporter);

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(postRepository.findById(10L)).willReturn(Optional.of(post));

            assertThatThrownBy(() ->
                    userReportService.reportPost(1L,
                            new ReportRequestDTO(10L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_400_REPORT_SELF);
        }

        @Test
        void 삭제된게시글() {

            given(post.isDeleted()).willReturn(true);

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(postRepository.findById(10L)).willReturn(Optional.of(post));

            assertThatThrownBy(() ->
                    userReportService.reportPost(1L,
                            new ReportRequestDTO(10L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_410_ALREADY_DELETED);
        }

        @Test
        void 중복신고() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(postRepository.findById(10L)).willReturn(Optional.of(post));
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(
                    reporter, TargetType.POST, 10L
            )).willReturn(true);

            assertThatThrownBy(() ->
                    userReportService.reportPost(1L,
                            new ReportRequestDTO(10L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_409_ALREADY_REPORT_USER);
        }
    }

    // =========================================================
    // COMMENT 신고
    // =========================================================
    @Nested
    class CommentReport {

        @Test
        void 성공() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(20L)).willReturn(Optional.of(comment));
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(
                    reporter, TargetType.COMMENT, 20L
            )).willReturn(false);

            userReportService.reportComment(1L,
                    new ReportRequestDTO(20L, "ABUSE", "x"));

            verify(reportRepository).save(any(Report.class));
        }

        @Test
        void 댓글없음() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(20L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userReportService.reportComment(1L,
                            new ReportRequestDTO(20L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_404_TARGET);
        }

        @Test
        void 본인댓글() {

            given(comment.getUserId()).willReturn(1L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    userReportService.reportComment(1L,
                            new ReportRequestDTO(20L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_400_REPORT_SELF);
        }

        @Test
        void 삭제된댓글() {

            given(comment.isDeleted()).willReturn(true);

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    userReportService.reportComment(1L,
                            new ReportRequestDTO(20L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_410_ALREADY_DELETED);
        }

        @Test
        void 중복신고() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(20L)).willReturn(Optional.of(comment));
            given(reportRepository.existsByReporterAndTargetTypeAndTargetId(
                    reporter, TargetType.COMMENT, 20L
            )).willReturn(true);

            assertThatThrownBy(() ->
                    userReportService.reportComment(1L,
                            new ReportRequestDTO(20L, "ABUSE", "x"))
            )
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ReportErrorCode.REPORT_409_ALREADY_REPORT_USER);
        }
    }
}