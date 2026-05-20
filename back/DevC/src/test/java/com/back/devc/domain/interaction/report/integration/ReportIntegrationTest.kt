package com.back.devc.domain.interaction.report.integration

import com.back.devc.domain.interaction.notification.entity.Notification
import com.back.devc.domain.interaction.notification.repository.NotificationRepository
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.Report.Companion.create
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalAdminMember
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalMember
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.security.jwt.JwtPrincipal
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.List

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Report Integration")
internal class ReportIntegrationTest {
    @Autowired
    private val mvc: MockMvc? = null

    @Autowired
    private val memberRepository: MemberRepository? = null

    @Autowired
    private val categoryRepository: CategoryRepository? = null

    @Autowired
    private val postRepository: PostRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    private val reportRepository: ReportRepository? = null

    @Autowired
    private val reportGroupRepository: ReportGroupRepository? = null

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    private var reporter: Member? = null
    private var author: Member? = null
    private var secondReporter: Member? = null
    private var admin: Member? = null
    private var category: Category? = null

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        val suffix = UUID.randomUUID().toString().substring(0, 8)

        reporter = memberRepository!!.save<Member>(
            createLocalMember(
                "report-integration-user-" + suffix + "@test.com",
                "password123!",
                "reporter-" + suffix
            )
        )
        author = memberRepository.save<Member>(
            createLocalMember(
                "report-integration-author-" + suffix + "@test.com",
                "password123!",
                "author-" + suffix
            )
        )
        secondReporter = memberRepository.save<Member>(
            createLocalMember(
                "report-integration-second-" + suffix + "@test.com",
                "password123!",
                "secondReporter-" + suffix
            )
        )
        admin = memberRepository.save<Member>(
            createLocalAdminMember(
                "report-integration-admin-" + suffix + "@test.com",
                "password123!",
                "admin-" + suffix
            )
        )
        category = categoryRepository!!.save<Category>(Category("report-integration-category-" + suffix))
    }

    @Test
    @DisplayName("user can report another member's post and the report is persisted")
    @Throws(Exception::class)
    fun reportPost_persistsReport() {
        setAuthentication(reporter!!, "ROLE_USER")
        val post = createPost(author!!, "reported post", "reported post content")

        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "targetId": %d,
                                  "reasonType": "SPAM",
                                  "reasonDetail": "Repeated promotion"
                                }
                                
                                """.trimIndent().formatted(post.postId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_201"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())

        val reports: MutableList<Report> =
            reportRepository!!.findAllByTargetTypeAndTargetId(TargetType.POST, post.postId!!)
        Assertions.assertThat<Report?>(reports).hasSize(1)
        val savedReport: Report = reports.getFirst()
        Assertions.assertThat(savedReport.reporter.userId).isEqualTo(reporter!!.userId)
        Assertions.assertThat<ReportStatus>(savedReport.status).isEqualTo(ReportStatus.PENDING)
        Assertions.assertThat(savedReport.reasonType).isEqualTo("SPAM")
        Assertions.assertThat(savedReport.reasonDetail).isEqualTo("Repeated promotion")
    }

    @Test
    @DisplayName("same user cannot report the same post twice")
    @Throws(Exception::class)
    fun reportPost_rejectsDuplicateReport() {
        setAuthentication(reporter!!, "ROLE_USER")
        val post = createPost(author!!, "duplicate target", "content")

        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportRequest(post.postId, "SPAM"))
        )
            .andExpect(MockMvcResultMatchers.status().isCreated())

        mvc.perform(
            MockMvcRequestBuilders.post("/api/report/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportRequest(post.postId, "SPAM"))
        )
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_409"))

        Assertions.assertThat<Report>(reportRepository!!.findAllByTargetTypeAndTargetId(TargetType.POST, post.postId!!))
            .hasSize(1)
    }

    @Test
    @DisplayName("user cannot report own comment")
    @Throws(Exception::class)
    fun reportComment_rejectsSelfReport() {
        setAuthentication(author!!, "ROLE_USER")
        val post = createPost(author!!, "own comment post", "content")
        val comment =
            commentRepository!!.save<Comment>(Comment.create(post.postId!!, author!!.userId!!, null, "own comment"))

        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/report/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportRequest(comment.id, "ABUSE"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_400"))

        Assertions.assertThat<Report>(
            reportRepository!!.findAllByTargetTypeAndTargetId(
                TargetType.COMMENT,
                comment.id!!
            )
        )
            .isEmpty()
    }

    @Test
    @DisplayName("admin can view grouped reports and approve a group with target deletion, notification, and sanction")
    @Throws(
        Exception::class
    )
    fun adminApproveReportGroup_resolvesReportsAndHandlesTarget() {
        val post = createPost(author!!, "group target", "group target content")
        reportRepository!!.save<Report?>(report(reporter!!, TargetType.POST, post.postId!!, "SPAM", "spam"))
        reportRepository.save<Report?>(report(secondReporter!!, TargetType.POST, post.postId!!, "ABUSE", "abuse"))

        setAuthentication(admin!!, "ROLE_ADMIN")
        val from = LocalDateTime.now().minusDays(1)
        val to = LocalDateTime.now().plusDays(1)

        mvc!!.perform(
            MockMvcRequestBuilders.get("/api/admin/reports/groups")
                .param("status", "PENDING")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath<MutableCollection<*>?>(
                    "$.data.content",
                    Matchers.hasSize<Any?>(1)
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetType").value("POST"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetId").value(post.postId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetNickname").value(author!!.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].targetTitle").value("group target"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].reportCount").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath<MutableCollection<*>?>(
                    "$.data.content[0].reasonTypes",
                    Matchers.hasSize<Any?>(2)
                )
            )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/admin/reports/groups/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "reportId": %d,
                                  "targetType": "POST",
                                  "adminNote": "confirmed",
                                  "sanctionType": "WARNED",
                                  "suspensionDays": null
                                }
                                
                                """.trimIndent().formatted(post.postId)
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REPORT_200"))

        val resolvedReports: MutableList<Report> =
            reportRepository.findAllByTargetTypeAndTargetId(TargetType.POST, post.postId!!)
        Assertions.assertThat<Report?>(resolvedReports)
            .hasSize(2)
            .allSatisfy(ThrowingConsumer { report: Report? ->
                Assertions.assertThat<ReportStatus>(report!!.status).isEqualTo(ReportStatus.RESOLVED)
                Assertions.assertThat(report.processedByAdmin!!.userId).isEqualTo(admin!!.userId)
                Assertions.assertThat(report.processedAt).isNotNull()
            })

        val deletedPost = postRepository!!.findById(post.postId).orElseThrow()
        Assertions.assertThat(deletedPost.isDeleted).isTrue()

        val sanctionedAuthor = memberRepository!!.findById(author!!.userId).orElseThrow()
        Assertions.assertThat<MemberStatus>(sanctionedAuthor.status).isEqualTo(MemberStatus.WARNED)

        Assertions.assertThat<Notification>(notificationRepository!!.findByUserIdOrderByCreatedAtDesc(author!!.userId!!))
            .anySatisfy(ThrowingConsumer { notification: Notification? ->
                Assertions.assertThat(notification!!.type).isEqualTo("REPORT")
                Assertions.assertThat(notification.actorUserId).isEqualTo(admin!!.userId)
                Assertions.assertThat(notification.postId).isEqualTo(post.postId)
            })
    }

    private fun setAuthentication(member: Member, role: String) {
        val principal = JwtPrincipal(
            member.userId!!,
            member.email,
            role.replace("ROLE_", "")
        )

        val auth: Authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of<SimpleGrantedAuthority?>(SimpleGrantedAuthority(role))
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.setAuthentication(auth)
        SecurityContextHolder.setContext(context)
    }

    private fun createPost(member: Member, title: String, content: String): Post {
        return postRepository!!.save<Post>(Post.create(member, category!!, title, content))
    }

    private fun report(
        reporter: Member,
        targetType: TargetType,
        targetId: Long,
        reasonType: String,
        reasonDetail: String?
    ): Report {
        val report = create(
            reporter,
            targetType,
            targetId,
            reasonType,
            reasonDetail
        )
        report.assignReportGroup(reportGroup(targetType, targetId))
        return report
    }

    private fun reportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()

        var reportGroup = reportGroupRepository!!
            .findByTargetTypeAndTargetId(targetType, targetId)

        if (reportGroup == null) {
            reportGroup = ReportGroup(targetType, targetId, reportedAt)
        }

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush<ReportGroup>(reportGroup)
    }

    private fun reportRequest(targetId: Long?, reasonType: String?): String {
        return """
                {
                  "targetId": %d,
                  "reasonType": "%s",
                  "reasonDetail": "detail"
                }
                
                """.trimIndent().formatted(targetId, reasonType)
    }
}
