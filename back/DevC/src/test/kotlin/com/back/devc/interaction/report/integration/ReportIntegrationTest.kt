package com.back.devc.interaction.report.integration

import com.back.devc.domain.interaction.notification.repository.NotificationRepository
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.interaction.report.orThrow
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Report Integration")
internal class ReportIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val memberRepository: MemberRepository,
    private val categoryRepository: CategoryRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val reportRepository: ReportRepository,
    private val reportGroupRepository: ReportGroupRepository,
    private val notificationRepository: NotificationRepository
) {
    private lateinit var reporter: Member
    private lateinit var author: Member
    private lateinit var secondReporter: Member
    private lateinit var admin: Member
    private lateinit var category: Category

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        val suffix = UUID.randomUUID().toString().substring(0, 8)

        reporter = saveUser("report-integration-user-$suffix@test.com", "reporter-$suffix")
        author = saveUser("report-integration-author-$suffix@test.com", "author-$suffix")
        secondReporter = saveUser("report-integration-second-$suffix@test.com", "secondReporter-$suffix")
        admin = memberRepository.save(
            Member.createLocalAdminMember(
                email = "report-integration-admin-$suffix@test.com",
                passwordHash = "password123!",
                nickname = "admin-$suffix"
            )
        )
        category = categoryRepository.save(Category("report-integration-category-$suffix"))
    }

    @Test
    @DisplayName("user can report another member's post and the report is persisted")
    fun reportPost_persistsReport() {
        setAuthentication(reporter, "ROLE_USER")
        val post = createPost(author, "reported post", "reported post content")
        val postId = requireNotNull(post.postId)

        mvc.perform(
            post("/api/report/post")
                .contentType(APPLICATION_JSON)
                .content(reportRequest(postId, "SPAM", "Repeated promotion"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("REPORT_201"))
            .andExpect(jsonPath("$.data").doesNotExist())

        val reports = reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, postId)
        val savedReport = reports.single()

        assertThat(savedReport.reporter.userId).isEqualTo(reporter.userId)
        assertThat(savedReport.status).isEqualTo(ReportStatus.PENDING)
        assertThat(savedReport.reasonType).isEqualTo("SPAM")
        assertThat(savedReport.reasonDetail).isEqualTo("Repeated promotion")
    }

    @Test
    @DisplayName("same user cannot report the same post twice")
    fun reportPost_rejectsDuplicateReport() {
        setAuthentication(reporter, "ROLE_USER")
        val post = createPost(author, "duplicate target", "content")
        val postId = requireNotNull(post.postId)

        mvc.perform(
            post("/api/report/post")
                .contentType(APPLICATION_JSON)
                .content(reportRequest(postId, "SPAM"))
        ).andExpect(status().isCreated)

        mvc.perform(
            post("/api/report/post")
                .contentType(APPLICATION_JSON)
                .content(reportRequest(postId, "SPAM"))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("REPORT_409"))

        assertThat(reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, postId))
            .hasSize(1)
    }

    @Test
    @DisplayName("user cannot report own comment")
    fun reportComment_rejectsSelfReport() {
        setAuthentication(author, "ROLE_USER")
        val post = createPost(author, "own comment post", "content")
        val comment = commentRepository.save(
            Comment.create(
                postId = requireNotNull(post.postId),
                userId = requireNotNull(author.userId),
                parentCommentId = null,
                content = "own comment"
            )
        )
        val commentId = requireNotNull(comment.id)

        mvc.perform(
            post("/api/report/comment")
                .contentType(APPLICATION_JSON)
                .content(reportRequest(commentId, "ABUSE"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REPORT_400"))

        assertThat(reportRepository.findAllByTargetTypeAndTargetId(TargetType.COMMENT, commentId))
            .isEmpty()
    }

    @Test
    @DisplayName("admin can view grouped reports and approve a group with target deletion, notification, and sanction")
    fun adminApproveReportGroup_resolvesReportsAndHandlesTarget() {
        val post = createPost(author, "group target", "group target content")
        val postId = requireNotNull(post.postId)

        reportRepository.save(report(reporter, TargetType.POST, postId, "SPAM", "spam"))
        reportRepository.save(report(secondReporter, TargetType.POST, postId, "ABUSE", "abuse"))

        setAuthentication(admin, "ROLE_ADMIN")
        val from = LocalDateTime.now().minusDays(1)
        val to = LocalDateTime.now().plusDays(1)

        mvc.perform(
            get("/api/admin/reports/groups")
                .param("status", "PENDING")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("REPORT_200"))
            .andExpect(jsonPath("$.data.content", hasSize<Any>(1)))
            .andExpect(jsonPath("$.data.content[0].targetType").value("POST"))
            .andExpect(jsonPath("$.data.content[0].targetId").value(postId))
            .andExpect(jsonPath("$.data.content[0].targetNickname").value(author.nickname))
            .andExpect(jsonPath("$.data.content[0].targetTitle").value("group target"))
            .andExpect(jsonPath("$.data.content[0].reportCount").value(2))
            .andExpect(jsonPath("$.data.content[0].reasonTypes", hasSize<Any>(2)))

        mvc.perform(
            post("/api/admin/reports/groups/approve")
                .contentType(APPLICATION_JSON)
                .content(approveGroupRequest(postId))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("REPORT_200"))

        val resolvedReports = reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, postId)
        assertThat(resolvedReports)
            .hasSize(2)
            .allSatisfy { report ->
                assertThat(report.status).isEqualTo(ReportStatus.RESOLVED)
                assertThat(report.processedByAdmin?.userId).isEqualTo(admin.userId)
                assertThat(report.processedAt).isNotNull()
            }

        val deletedPost = postRepository.findById(postId).orThrow()
        assertThat(deletedPost.isDeleted).isTrue()

        val sanctionedAuthor = memberRepository.findById(requireNotNull(author.userId)).orThrow()
        assertThat(sanctionedAuthor.status).isEqualTo(MemberStatus.WARNED)

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(requireNotNull(author.userId)))
            .anySatisfy { notification ->
                assertThat(notification.type).isEqualTo("REPORT")
                assertThat(notification.actorUserId).isEqualTo(admin.userId)
                assertThat(notification.postId).isEqualTo(postId)
            }
    }

    private fun saveUser(email: String, nickname: String): Member =
        memberRepository.save(
            Member.createLocalMember(
                email = email,
                passwordHash = "password123!",
                nickname = nickname
            )
        )

    private fun setAuthentication(member: Member, role: String) {
        val principal = JwtPrincipal(
            userId = requireNotNull(member.userId),
            email = member.email,
            role = role.removePrefix("ROLE_")
        )
        val auth = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority(role))
        )

        SecurityContextHolder.getContext().authentication = auth
    }

    private fun createPost(member: Member, title: String, content: String): Post =
        postRepository.save(Post.create(member, category, title, content))

    private fun report(
        reporter: Member,
        targetType: TargetType,
        targetId: Long,
        reasonType: String,
        reasonDetail: String?
    ): Report {
        val report = Report.create(
            reporter = reporter,
            targetType = targetType,
            targetId = targetId,
            reasonType = reasonType,
            reasonDetail = reasonDetail
        )
        report.assignReportGroup(reportGroup(targetType, targetId))
        return report
    }

    private fun reportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()
        val reportGroup = reportGroupRepository.findByTargetTypeAndTargetId(targetType, targetId)
            ?: ReportGroup(targetType, targetId, reportedAt)

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush(reportGroup)
    }

    private fun reportRequest(
        targetId: Long,
        reasonType: String,
        reasonDetail: String = "detail"
    ): String =
        """
        {
          "targetId": $targetId,
          "reasonType": "$reasonType",
          "reasonDetail": "$reasonDetail"
        }
        """.trimIndent()

    private fun approveGroupRequest(postId: Long): String =
        """
        {
          "reportId": $postId,
          "targetType": "POST",
          "adminNote": "confirmed",
          "sanctionType": "WARNED",
          "suspensionDays": null
        }
        """.trimIndent()
}
