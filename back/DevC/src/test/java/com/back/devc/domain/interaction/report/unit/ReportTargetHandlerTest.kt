package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.service.MemberSanctionService
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class ReportTargetHandlerTest {
    @Mock
    var postRepository: PostRepository? = null

    @Mock
    var commentRepository: CommentRepository? = null

    @Mock
    var memberRepository: MemberRepository? = null

    @Mock
    var notificationService: NotificationService? = null

    @Mock
    var memberSanctionService: MemberSanctionService? = null

    @InjectMocks
    var handler: ReportTargetHandler? = null


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
    fun handleApproved_post_noSanction() {
        // 1. Given: 필요한 객체들만 모킹
        val admin = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<Long?>(admin.userId).thenReturn(1L)

        val post = Mockito.mock<Post>(Post::class.java)
        // post.getMember() 호출이 실제 로직에서 발생하지 않는다면 writer 모킹은 필요 없습니다.
        Mockito.`when`<Boolean?>(post.isDeleted).thenReturn(false)

        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L))
            .thenReturn(Optional.of<Post>(post))

        // 2. When: 테스트 대상 메서드 실행
        handler!!.handleApproved(
            TargetType.POST,
            10L,
            admin,
            null,  // sanctionType이 null이므로 memberSanctionService는 호출되지 않아야 함
            null
        )

        // 3. Then: 행위 검증
        // 알림 서비스가 호출되었는지 확인
        Mockito.verify<NotificationService?>(notificationService)
            .createPostReportNotification(10L, 1L)

        // 리포지토리 조회 및 게시글 삭제(soft delete 등) 호출 확인
        Mockito.verify<PostRepository?>(postRepository).findById(10L)
        Mockito.verify<Post?>(post).delete()

        // 제재 서비스는 호출되지 않았음을 확신
        Mockito.verifyNoInteractions(memberSanctionService)
    }

    //2. handleApproved - COMMENT + sanction
    @Test
    fun handleApproved_comment_withSanction() {
        val admin = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<Long?>(admin.userId).thenReturn(1L)

        val comment = Mockito.mock<Comment>(Comment::class.java)
        val writer = Mockito.mock<Member>(Member::class.java)

        Mockito.`when`<Boolean?>(comment.isDeleted).thenReturn(false)
        Mockito.`when`<Long?>(comment.getUserId()).thenReturn(99L)

        Mockito.`when`<Optional<Comment>?>(commentRepository!!.findById(20L))
            .thenReturn(Optional.of<Comment>(comment))

        Mockito.`when`<Optional<Member>?>(memberRepository!!.findById(99L))
            .thenReturn(Optional.of<Member>(writer))

        handler!!.handleApproved(
            TargetType.COMMENT,
            20L,
            admin,
            SanctionType.WARNED,
            0
        )

        Mockito.verify<NotificationService?>(notificationService)
            .createCommentReportNotification(20L, 1L)

        Mockito.verify<Comment?>(comment).softDelete()

        Mockito.verify<MemberSanctionService?>(memberSanctionService)
            .apply(writer, MemberStatus.WARNED, 0)
    }

    // 3. deleteTarget - POST already deleted 상태
    @Test
    fun deleteTarget_post_alreadyDeleted() {
        val post = Mockito.mock<Post>(Post::class.java)
        Mockito.`when`<Boolean?>(post.isDeleted).thenReturn(true)

        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L))
            .thenReturn(Optional.of<Post>(post))

        handler!!.handleApproved(
            TargetType.POST,
            10L,
            Mockito.mock<Member?>(Member::class.java),
            null,
            null
        )

        Mockito.verify<Post?>(post, Mockito.never()).delete()
    }

    // 4. COMMENT 삭제 케이스
    @Test
    fun deleteTarget_comment_success() {
        val comment = Mockito.mock<Comment>(Comment::class.java)
        Mockito.`when`<Boolean?>(comment.isDeleted).thenReturn(false)

        Mockito.`when`<Optional<Comment>?>(commentRepository!!.findById(20L))
            .thenReturn(Optional.of<Comment>(comment))

        handler!!.handleApproved(
            TargetType.COMMENT,
            20L,
            Mockito.mock<Member?>(Member::class.java),
            null,
            null
        )

        Mockito.verify<Comment?>(comment).softDelete()
    }

    // 5. applySanction - SUSPENDED
    @Test
    fun applySanction_suspended() {
        val writer = Mockito.mock<Member?>(Member::class.java)

        val post = Mockito.mock<Post>(Post::class.java)
        Mockito.`when`<Member>(post.member).thenReturn(writer)

        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L))
            .thenReturn(Optional.of<Post>(post))

        val admin = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<Long?>(admin.userId).thenReturn(1L)

        handler!!.handleApproved(
            TargetType.POST,
            10L,
            admin,
            SanctionType.SUSPENDED,
            7
        )

        Mockito.verify<MemberSanctionService?>(memberSanctionService)
            .apply(writer!!, MemberStatus.SUSPENDED, 7)
    }

    // 6. findTargetMember - POST 대상이 존재하지 않는 경우
    @Test
    fun findTargetMember_post_notFound() {
        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L))
            .thenReturn(Optional.empty<Post>())

        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable {
            handler!!.handleApproved(
                TargetType.POST,
                10L,
                Mockito.mock<Member?>(Member::class.java),
                SanctionType.WARNED,
                null
            )
        }
        ).isInstanceOf(ApiException::class.java)
    }

    // 7. COMMENT → member 없을 때 예외
    @Test
    fun findTargetMember_comment_memberNotFound() {
        val comment = Mockito.mock<Comment>(Comment::class.java)
        Mockito.`when`<Long?>(comment.getUserId()).thenReturn(99L)

        Mockito.`when`<Optional<Comment>?>(commentRepository!!.findById(20L))
            .thenReturn(Optional.of<Comment>(comment))

        Mockito.`when`<Optional<Member>?>(memberRepository!!.findById(99L))
            .thenReturn(Optional.empty<Member>())

        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable {
            handler!!.handleApproved(
                TargetType.COMMENT,
                20L,
                Mockito.mock<Member?>(Member::class.java),
                SanctionType.WARNED,
                null
            )
        }
        ).isInstanceOf(ApiException::class.java)
    }

    // 8. handleRejected
    @Test
    fun handleRejected_success() {
        val admin = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<Long?>(admin.userId).thenReturn(1L)

        handler!!.handleRejected(
            TargetType.POST,
            10L,
            admin
        )

        Mockito.verify<NotificationService?>(notificationService)
            .createPostReportNotification(10L, 1L)
    }

    // 10. exists 테스트
    @Test
    fun exists_post_true() {
        Mockito.`when`<Boolean?>(postRepository!!.existsById(10L))
            .thenReturn(true)

        val result = handler!!.exists(TargetType.POST, 10L)

        Assertions.assertThat(result).isTrue()
    }

    @Test
    fun exists_comment_false() {
        Mockito.`when`<Boolean?>(commentRepository!!.existsById(20L))
            .thenReturn(false)

        val result = handler!!.exists(TargetType.COMMENT, 20L)

        Assertions.assertThat(result).isFalse()
    }

    @Test
    fun getTargetInfo_post_success() {
        val writer = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<String>(writer.nickname).thenReturn("post-writer")

        val post = Mockito.mock<Post>(Post::class.java)
        Mockito.`when`<Member>(post.member).thenReturn(writer)
        Mockito.`when`<String>(post.title).thenReturn("post title")
        Mockito.`when`<String>(post.content).thenReturn("post content")
        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L)).thenReturn(Optional.of<Post>(post))

        val result = handler!!.getTargetInfo(TargetType.POST, 10L)

        Assertions.assertThat(result.nickname).isEqualTo("post-writer")
        Assertions.assertThat(result.title).isEqualTo("post title")
        Assertions.assertThat(result.content).isEqualTo("post content")
    }

    @Test
    fun getTargetInfo_comment_success() {
        val comment = Mockito.mock<Comment>(Comment::class.java)
        Mockito.`when`<Long?>(comment.getUserId()).thenReturn(99L)
        Mockito.`when`<String?>(comment.content).thenReturn("comment content")
        Mockito.`when`<Optional<Comment>?>(commentRepository!!.findById(20L)).thenReturn(Optional.of<Comment>(comment))

        val writer = Mockito.mock<Member>(Member::class.java)
        Mockito.`when`<String>(writer.nickname).thenReturn("comment-writer")
        Mockito.`when`<Optional<Member>?>(memberRepository!!.findById(99L)).thenReturn(Optional.of<Member>(writer))

        val result = handler!!.getTargetInfo(TargetType.COMMENT, 20L)

        Assertions.assertThat(result.nickname).isEqualTo("comment-writer")
        Assertions.assertThat(result.title).isNull()
        Assertions.assertThat(result.content).isEqualTo("comment content")
    }

    @Test
    fun getTargetInfo_missingTarget_returnsNullFields() {
        Mockito.`when`<Optional<Post>?>(postRepository!!.findById(10L)).thenReturn(Optional.empty<Post>())

        val result = handler!!.getTargetInfo(TargetType.POST, 10L)

        Assertions.assertThat(result.nickname).isNull()
        Assertions.assertThat(result.title).isNull()
        Assertions.assertThat(result.content).isNull()
    }
}
