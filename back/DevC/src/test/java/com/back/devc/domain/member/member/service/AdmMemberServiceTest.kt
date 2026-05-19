package com.back.devc.domain.member.member.service

import com.back.devc.domain.member.member.dto.AdmMemberListRequest
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import com.back.devc.domain.member.member.dto.CountResultDto
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.exception.errorCode.MemberErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("AdmMemberService")
class AdmMemberServiceTest {

    private val memberRepository = mock(MemberRepository::class.java)
    private val postRepository = mock(PostRepository::class.java)
    private val commentRepository = mock(CommentRepository::class.java)
    private val service = AdmMemberService(memberRepository, postRepository, commentRepository)

    @Test
    @DisplayName("getMembers combines post and comment counts by user")
    fun getMembersCombinesPostAndCommentCounts() {
        val member = member(1L, "member@test.com", "member", MemberStatus.ACTIVE)
        val request = AdmMemberListRequest(page = 0, size = 20, keyword = null, status = null)
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        `when`(memberRepository.findAll(pageable)).thenReturn(PageImpl(listOf(member)))
        `when`(postRepository.countPostsByUserIds(listOf(1L))).thenReturn(listOf(CountResultDto(1L, 3L)))
        `when`(commentRepository.countCommentsByUserIds(listOf(1L))).thenReturn(listOf(CountResultDto(1L, 5L)))

        val result = service.getMembers(request)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].postCount).isEqualTo(3L)
        assertThat(result.content[0].commentCount).isEqualTo(5L)
    }

    @Test
    @DisplayName("getMembers uses combined status and keyword search")
    fun getMembersUsesStatusAndKeywordSearch() {
        val request = AdmMemberListRequest(page = 0, size = 20, keyword = "nick", status = MemberStatus.ACTIVE)
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        `when`(
            memberRepository.findByStatusAndNicknameContainingOrStatusAndEmailContaining(
                MemberStatus.ACTIVE,
                "nick",
                MemberStatus.ACTIVE,
                "nick",
                pageable,
            ),
        ).thenReturn(PageImpl(emptyList()))

        service.getMembers(request)

        verify(memberRepository).findByStatusAndNicknameContainingOrStatusAndEmailContaining(
            MemberStatus.ACTIVE,
            "nick",
            MemberStatus.ACTIVE,
            "nick",
            pageable,
        )
    }

    @Test
    @DisplayName("getMemberDetail rejects withdrawn members")
    fun getMemberDetailRejectsWithdrawnMember() {
        val member = member(1L, "withdrawn@test.com", "withdrawn", MemberStatus.WITHDRAWN)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))

        assertThatThrownBy { service.getMemberDetail(1L) }
            .isInstanceOf(ApiException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND)
    }

    @Test
    @DisplayName("getMemberDetail rejects blacklisted members")
    fun getMemberDetailRejectsBlacklistedMember() {
        val member = member(1L, "black@test.com", "black", MemberStatus.BLACKLISTED)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))

        assertThatThrownBy { service.getMemberDetail(1L) }
            .isInstanceOf(ApiException::class.java)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.MEMBER_BLACKLISTED)
    }

    @Test
    @DisplayName("updateMemberStatus changes status and returns counts")
    fun updateMemberStatusChangesStatusAndReturnsCounts() {
        val member = member(1L, "active@test.com", "active", MemberStatus.ACTIVE)
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        `when`(postRepository.countPostsByUserIds(listOf(1L))).thenReturn(listOf(CountResultDto(1L, 2L)))
        `when`(commentRepository.countCommentsByUserIds(listOf(1L))).thenReturn(listOf(CountResultDto(1L, 4L)))

        val result = service.updateMemberStatus(
            1L,
            AdmMemberStatusUpdateRequest(status = MemberStatus.WARNED, days = null),
        )

        assertThat(member.status).isEqualTo(MemberStatus.WARNED)
        assertThat(result.status).isEqualTo(MemberStatus.WARNED)
        assertThat(result.postCount).isEqualTo(2L)
        assertThat(result.commentCount).isEqualTo(4L)
    }

    @Test
    @DisplayName("missing member throws common member not found")
    fun missingMemberThrowsCommonNotFound() {
        `when`(memberRepository.findById(404L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.getMemberDetail(404L) }
            .isInstanceOf(ApiException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
    }

    private fun member(
        userId: Long,
        email: String,
        nickname: String,
        status: MemberStatus,
    ): Member {
        return Member.createLocalMember(email, "password", nickname).also {
            ReflectionTestUtils.setField(it, "userId", userId)
            ReflectionTestUtils.setField(it, "status", status)
            ReflectionTestUtils.setField(it, "createdAt", LocalDateTime.now())
            ReflectionTestUtils.setField(it, "updatedAt", LocalDateTime.now())
        }
    }
}

