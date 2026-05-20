package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import kotlin.math.min

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class ReportPerformanceTest {
    @InjectMocks
    private val admReportService: AdminReportService? = null

    @Mock
    private val reportRepository: ReportRepository? = null

    @Mock
    private val reportGroupRepository: ReportGroupRepository? = null

    @Mock
    private val memberRepository: MemberRepository? = null

    @Mock
    private val postRepository: PostRepository? = null

    @Mock
    private val commentRepository: CommentRepository? = null

    @Mock
    private val reportTargetHandler: ReportTargetHandler? = null


    @Test
    @DisplayName("신고 그룹 목록 조회 성능 측정 (데이터 200건 기준)")
    fun getGroupedReportsPerformanceTest() {
        // 1. Given: 페이징 및 데이터 양 설정
        val status = ReportStatus.PENDING
        val pageNumber = 400
        val pageSize = 20

        val totalData = 10000 // 전체 데이터 양
        val pageable = PageRequest.of(pageNumber, pageSize)

        val mockRows: MutableList<Array<Any?>?> = ArrayList<Array<Any?>?>()
        val postIds: MutableList<Long?> = ArrayList<Long?>()

        // 200개의 신고 그룹 데이터 생성 (실제 DB에 200줄이 있다고 가정)
        for (i in 1..totalData) {
            mockRows.add(
                arrayOf<Any>(
                    TargetType.POST,
                    i,  // targetId
                    5L,  // reportCount
                    LocalDateTime.now()
                )
            )
            postIds.add(i)
        }

        // PageImpl의 세 번째 인자를 totalData(200)로 설정
        // 실제 서비스 로직에서는 이 중 '첫 번째 페이지(20개)'만 처리하게 됩니다.
        val start = pageable.getOffset().toInt()
        val end = min(start + pageSize, mockRows.size)

        val pageRows = mockRows.subList(start, end)

        val mockPage: Page<Array<Any?>?> = PageImpl<Array<Any?>?>(pageRows, pageable, totalData.toLong())

        BDDMockito.given<Page<Array<Any>>>(
            reportRepository!!.findGroupedReports(
                ArgumentMatchers.any<ReportStatus?>(ReportStatus::class.java),
                ArgumentMatchers.any<LocalDateTime?>(LocalDateTime::class.java),
                ArgumentMatchers.any<LocalDateTime?>(LocalDateTime::class.java),
                ArgumentMatchers.any<Pageable?>(Pageable::class.java)
            )
        ).willReturn(mockPage)

        // 2. Given: 엔티티 Mocking
        val mockMember = Mockito.mock<Member>(Member::class.java)
        BDDMockito.given<String>(mockMember.nickname).willReturn("작성자닉네임")

        // 서비스 로직에서 IN 절로 조회할 20개의 Post 상세 정보 Mocking
        val mockPosts = postIds.stream()
            .limit(pageSize.toLong()) // 현재 페이지에 해당하는 데이터만 있으면 됨
            .map<Post?> { id: Long? ->
                val p = Mockito.mock<Post>(Post::class.java)
                BDDMockito.given<Long?>(p.postId).willReturn(id)
                BDDMockito.given<String>(p.title).willReturn("제목 " + id)
                BDDMockito.given<Member>(p.member).willReturn(mockMember)
                p
            }
            .toList()

        BDDMockito.given<MutableList<Post>>(postRepository!!.findAllByPostIdIn(ArgumentMatchers.any<MutableList<Long>>()))
            .willReturn(mockPosts)

        // 3. Given: 기타 설정 (NPE 방지)
        Mockito.lenient()
            .`when`<MutableList<Comment>>(commentRepository!!.findAllByIdIn(ArgumentMatchers.any<MutableList<Long>>()))
            .thenReturn(
                mutableListOf<Comment>()
            )
        Mockito.lenient()
            .`when`<MutableList<Member>?>(memberRepository!!.findAllById(ArgumentMatchers.any<Iterable<Long>?>()))
            .thenReturn(
                mutableListOf<Member>()
            )

        // 4. When: 실행
        val sw = StopWatch()
        sw.start()

        val result: Page<ReportGroupResponseDTO?> =
            admReportService!!.getGroupedReports(status, pageable)

        sw.stop()

        // 5. Then
        println("====== 성능 측정 결과 ======")
        println("처리 방식: IN Batch (N+1 제거)")
        println("전체 데이터 수: " + result.getTotalElements())
        println("측정 시간: " + sw.getTotalTimeMillis() + "ms")
        println("==========================================")

        Assertions.assertThat<ReportGroupResponseDTO?>(result.getContent()).hasSize(pageSize)
        Assertions.assertThat(result.getTotalElements()).isEqualTo(totalData.toLong())
    }

    @Test
    @DisplayName("신고 그룹 목록 조회 성능 측정 (NO Batch - N+1 발생 구조)")
    fun getGroupedReportsNoBatchPerformanceTest() {
        // 1. Given

        val status = ReportStatus.PENDING
        val pageNumber = 400
        val pageSize = 20
        val totalData = 10000
        val pageable = PageRequest.of(pageNumber, pageSize)

        val mockRows: MutableList<Array<Any?>?> = ArrayList<Array<Any?>?>()
        val postIds: MutableList<Long?> = ArrayList<Long?>()

        for (i in 1..totalData) {
            mockRows.add(
                arrayOf<Any>(
                    TargetType.POST,
                    i,
                    5L,
                    LocalDateTime.now()
                )
            )
            postIds.add(i)
        }
        val start = pageable.getOffset().toInt()
        val end = min(start + pageSize, mockRows.size)

        val pageRows = mockRows.subList(start, end)

        val mockPage: Page<Array<Any?>?> = PageImpl<Array<Any?>?>(pageRows, pageable, totalData.toLong())

        BDDMockito.given<Page<Array<Any>>>(
            reportRepository!!.findGroupedReports(
                ArgumentMatchers.any<ReportStatus?>(ReportStatus::class.java),
                ArgumentMatchers.any<LocalDateTime?>(LocalDateTime::class.java),
                ArgumentMatchers.any<LocalDateTime?>(LocalDateTime::class.java),
                ArgumentMatchers.any<Pageable?>(Pageable::class.java)
            )
        ).willReturn(mockPage)

        // 2. target handler mock (NO batch 핵심)
        val mockInfo =
            ReportTargetHandler.TargetInfo("작성자닉네임", "제목", "내용")

        // row마다 호출됨 (N+1 구조 재현)
        for (row in pageRows) {
            val targetId = row!![1] as Long
            BDDMockito.given<ReportTargetHandler.TargetInfo>(
                reportTargetHandler!!.getTargetInfo(
                    TargetType.POST,
                    targetId
                )
            )
                .willReturn(mockInfo)
            BDDMockito.given<MutableList<String>>(
                reportRepository.findReasonTypesByTargetId(
                    TargetType.POST,
                    targetId
                )
            )
                .willReturn(mutableListOf<String>("SPAM", "ABUSE"))
        }

        // 3. When
        val sw = StopWatch()
        sw.start()

        val result: Page<ReportGroupResponseDTO?> =
            admReportService!!.getGroupedReportsNoBatch(status, pageable)

        sw.stop()

        // 4. Then
        println("====== NO BATCH 성능 측정 결과 ======")
        println("처리 방식: NO Batch (N+1 발생)")
        println("전체 데이터 수: " + result.getTotalElements())
        println("측정 시간: " + sw.getTotalTimeMillis() + "ms")
        println("====================================")

        Assertions.assertThat<ReportGroupResponseDTO?>(result.getContent()).hasSize(pageSize)
        Assertions.assertThat(result.getTotalElements()).isEqualTo(totalData.toLong())

        // 핵심 검증: row 수만큼 반복 호출됨 (N+1 구조)
        for (row in pageRows) {
            val targetId = row!![1] as Long
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler)
                .getTargetInfo(TargetType.POST, targetId)
            Mockito.verify<ReportRepository?>(reportRepository)
                .findReasonTypesByTargetId(TargetType.POST, targetId)
        }
    }
}
