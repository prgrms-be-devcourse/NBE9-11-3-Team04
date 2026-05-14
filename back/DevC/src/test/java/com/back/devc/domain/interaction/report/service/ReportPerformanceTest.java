package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.interaction.report.util.ReportTargetHandler;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportPerformanceTest {

    @InjectMocks
    private AdminReportService admReportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository; // 누락 주의
    @Mock
    private ReportTargetHandler reportTargetHandler; // 누락 주의


    @Test
    @DisplayName("신고 그룹 목록 조회 성능 측정 (데이터 200건 기준)")
    void getGroupedReportsPerformanceTest() {
        // 1. Given: 페이징 및 데이터 양 설정
        ReportStatus status = ReportStatus.PENDING;
        int pageNumber = 400;
        int pageSize = 20;

        int totalData = 10000;  // 전체 데이터 양
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);

        List<Object[]> mockRows = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        // 200개의 신고 그룹 데이터 생성 (실제 DB에 200줄이 있다고 가정)
        for (long i = 1; i <= totalData; i++) {
            mockRows.add(new Object[]{
                    TargetType.POST,
                    i,      // targetId
                    5L,     // reportCount
                    LocalDateTime.now()
            });
            postIds.add(i);
        }

        // PageImpl의 세 번째 인자를 totalData(200)로 설정
        // 실제 서비스 로직에서는 이 중 '첫 번째 페이지(20개)'만 처리하게 됩니다.
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageSize, mockRows.size());

        List<Object[]> pageRows = mockRows.subList(start, end);

        Page<Object[]> mockPage = new PageImpl<>(pageRows, pageable, totalData);

        given(reportRepository.findGroupedReports(any(), any())).willReturn(mockPage);

        // 2. Given: 엔티티 Mocking
        Member mockMember = mock(Member.class);
        given(mockMember.getNickname()).willReturn("작성자닉네임");

        // 서비스 로직에서 IN 절로 조회할 20개의 Post 상세 정보 Mocking
        List<Post> mockPosts = postIds.stream()
                .limit(pageSize) // 현재 페이지에 해당하는 데이터만 있으면 됨
                .map(id -> {
                    Post p = mock(Post.class);
                    given(p.getPostId()).willReturn(id);
                    given(p.getTitle()).willReturn("제목 " + id);
                    given(p.getMember()).willReturn(mockMember);
                    return p;
                })
                .toList();

        given(postRepository.findAllByPostIdIn(any())).willReturn(mockPosts);

        // 3. Given: 기타 설정 (NPE 방지)
        lenient().when(commentRepository.findAllByIdIn(any())).thenReturn(List.of());
        lenient().when(memberRepository.findAllByUserIdIn(any())).thenReturn(List.of());

        // 4. When: 실행
        StopWatch sw = new StopWatch();
        sw.start();

        Page<ReportGroupResponseDTO> result =
                admReportService.getGroupedReports(status, pageable);

        sw.stop();

        // 5. Then
        System.out.println("====== 성능 측정 결과 ======");
        System.out.println("처리 방식: IN Batch (N+1 제거)");
        System.out.println("전체 데이터 수: " + result.getTotalElements());
        System.out.println("측정 시간: " + sw.getTotalTimeMillis() + "ms");
        System.out.println("==========================================");

        assertThat(result.getContent()).hasSize(pageSize);
        assertThat(result.getTotalElements()).isEqualTo(totalData);
    }

    @Test
    @DisplayName("신고 그룹 목록 조회 성능 측정 (NO Batch - N+1 발생 구조)")
    void getGroupedReportsNoBatchPerformanceTest() {

        // 1. Given
        ReportStatus status = ReportStatus.PENDING;
        int pageNumber = 400;
        int pageSize = 20;
        int totalData = 10000;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);

        List<Object[]> mockRows = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        for (long i = 1; i <= totalData; i++) {
            mockRows.add(new Object[]{
                    TargetType.POST,
                    i,
                    5L,
                    LocalDateTime.now()
            });
            postIds.add(i);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageSize, mockRows.size());

        List<Object[]> pageRows = mockRows.subList(start, end);

        Page<Object[]> mockPage = new PageImpl<>(pageRows, pageable, totalData);

        given(reportRepository.findGroupedReports(any(), any()))
                .willReturn(mockPage);

        // 2. target handler mock (NO batch 핵심)
        ReportTargetHandler.TargetInfo mockInfo =
                new ReportTargetHandler.TargetInfo("작성자닉네임", "제목", "내용");

        // row마다 호출됨 (N+1 구조 재현)
        given(reportTargetHandler.getTargetInfo(any(), any()))
                .willReturn(mockInfo);

        // reasonTypes도 row마다 호출됨
        given(reportRepository.findReasonTypesByTargetId(any(), any()))
                .willReturn(List.of("SPAM", "ABUSE"));

        // 3. When
        StopWatch sw = new StopWatch();
        sw.start();

        Page<ReportGroupResponseDTO> result =
                admReportService.getGroupedReportsNoBatch(status, pageable);

        sw.stop();

        // 4. Then
        System.out.println("====== NO BATCH 성능 측정 결과 ======");
        System.out.println("처리 방식: NO Batch (N+1 발생)");
        System.out.println("전체 데이터 수: " + result.getTotalElements());
        System.out.println("측정 시간: " + sw.getTotalTimeMillis() + "ms");
        System.out.println("====================================");

        assertThat(result.getContent()).hasSize(pageSize);
        assertThat(result.getTotalElements()).isEqualTo(totalData);

        // 핵심 검증: row 수만큼 반복 호출됨 (N+1 구조)
        org.mockito.Mockito.verify(reportTargetHandler, org.mockito.Mockito.times(pageSize))
                .getTargetInfo(any(), any());

        org.mockito.Mockito.verify(reportRepository, org.mockito.Mockito.times(pageSize))
                .findReasonTypesByTargetId(any(), any());
    }
}