package com.back.devc.domain.interaction.report;

import com.back.devc.domain.interaction.notification.repository.NotificationRepository;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Report Integration")
class ReportIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Member reporter;
    private Member author;
    private Member secondReporter;
    private Member admin;
    private Category category;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        reporter = memberRepository.save(Member.createLocalMember(
                "report-integration-user-" + suffix + "@test.com",
                "password123!",
                "reporter-" + suffix
        ));
        author = memberRepository.save(Member.createLocalMember(
                "report-integration-author-" + suffix + "@test.com",
                "password123!",
                "author-" + suffix
        ));
        secondReporter = memberRepository.save(Member.createLocalMember(
                "report-integration-second-" + suffix + "@test.com",
                "password123!",
                "secondReporter-" + suffix
        ));
        admin = memberRepository.save(Member.createLocalAdminMember(
                "report-integration-admin-" + suffix + "@test.com",
                "password123!",
                "admin-" + suffix
        ));
        category = categoryRepository.save(new Category("report-integration-category-" + suffix));
    }

    @Test
    @DisplayName("user can report another member's post and the report is persisted")
    void reportPost_persistsReport() throws Exception {
        setAuthentication(reporter, "ROLE_USER");
        Post post = createPost(author, "reported post", "reported post content");

        mvc.perform(post("/api/report/post")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %d,
                                  "reasonType": "SPAM",
                                  "reasonDetail": "Repeated promotion"
                                }
                                """.formatted(post.getPostId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REPORT_201"))
                .andExpect(jsonPath("$.data").doesNotExist());

        List<Report> reports = reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, post.getPostId());
        assertThat(reports).hasSize(1);
        Report savedReport = reports.getFirst();
        assertThat(savedReport.getReporter().getUserId()).isEqualTo(reporter.getUserId());
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(savedReport.getReasonType()).isEqualTo("SPAM");
        assertThat(savedReport.getReasonDetail()).isEqualTo("Repeated promotion");
    }

    @Test
    @DisplayName("same user cannot report the same post twice")
    void reportPost_rejectsDuplicateReport() throws Exception {
        setAuthentication(reporter, "ROLE_USER");
        Post post = createPost(author, "duplicate target", "content");

        mvc.perform(post("/api/report/post")
                        .contentType(APPLICATION_JSON)
                        .content(reportRequest(post.getPostId(), "SPAM")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/report/post")
                        .contentType(APPLICATION_JSON)
                        .content(reportRequest(post.getPostId(), "SPAM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_409"));

        assertThat(reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, post.getPostId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("user cannot report own comment")
    void reportComment_rejectsSelfReport() throws Exception {
        setAuthentication(author, "ROLE_USER");
        Post post = createPost(author, "own comment post", "content");
        Comment comment = commentRepository.save(Comment.create(post.getPostId(), author.getUserId(), null, "own comment"));

        mvc.perform(post("/api/report/comment")
                        .contentType(APPLICATION_JSON)
                        .content(reportRequest(comment.getId(), "ABUSE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_400"));

        assertThat(reportRepository.findAllByTargetTypeAndTargetId(TargetType.COMMENT, comment.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("admin can view grouped reports and approve a group with target deletion, notification, and sanction")
    void adminApproveReportGroup_resolvesReportsAndHandlesTarget() throws Exception {
        Post post = createPost(author, "group target", "group target content");
        reportRepository.save(report(reporter, TargetType.POST, post.getPostId(), "SPAM", "spam"));
        reportRepository.save(report(secondReporter, TargetType.POST, post.getPostId(), "ABUSE", "abuse"));

        setAuthentication(admin, "ROLE_ADMIN");
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        mvc.perform(get("/api/admin/reports/groups")
                        .param("status", "PENDING")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_200"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.content[0].targetNickname").value(author.getNickname()))
                .andExpect(jsonPath("$.data.content[0].targetTitle").value("group target"))
                .andExpect(jsonPath("$.data.content[0].reportCount").value(2))
                .andExpect(jsonPath("$.data.content[0].reasonTypes", hasSize(2)));

        mvc.perform(post("/api/admin/reports/groups/approve")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": %d,
                                  "targetType": "POST",
                                  "adminNote": "confirmed",
                                  "sanctionType": "WARNED",
                                  "suspensionDays": null
                                }
                                """.formatted(post.getPostId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_200"));

        List<Report> resolvedReports = reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, post.getPostId());
        assertThat(resolvedReports)
                .hasSize(2)
                .allSatisfy(report -> {
                    assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
                    assertThat(report.getProcessedByAdmin().getUserId()).isEqualTo(admin.getUserId());
                    assertThat(report.getProcessedAt()).isNotNull();
                });

        Post deletedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(deletedPost.isDeleted()).isTrue();

        Member sanctionedAuthor = memberRepository.findById(author.getUserId()).orElseThrow();
        assertThat(sanctionedAuthor.getStatus()).isEqualTo(MemberStatus.WARNED);

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(author.getUserId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getType()).isEqualTo("REPORT");
                    assertThat(notification.getActorUserId()).isEqualTo(admin.getUserId());
                    assertThat(notification.getPostId()).isEqualTo(post.getPostId());
                });
    }

    private void setAuthentication(Member member, String role) {
        JwtPrincipal principal = new JwtPrincipal(
                member.getUserId(),
                member.getEmail(),
                role.replace("ROLE_", "")
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private Post createPost(Member member, String title, String content) {
        return postRepository.save(Post.create(member, category, title, content));
    }

    private Report report(Member reporter, TargetType targetType, Long targetId, String reasonType, String reasonDetail) {
        return Report.create(
                reporter,
                targetType,
                targetId,
                reasonType,
                reasonDetail
        );
    }

    private String reportRequest(Long targetId, String reasonType) {
        return """
                {
                  "targetId": %d,
                  "reasonType": "%s",
                  "reasonDetail": "detail"
                }
                """.formatted(targetId, reasonType);
    }
}
