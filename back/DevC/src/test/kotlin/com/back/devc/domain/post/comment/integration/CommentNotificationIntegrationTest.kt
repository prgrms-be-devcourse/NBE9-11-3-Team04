package com.back.devc.domain.post.comment.integration

import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.post.comment.dto.CommentCreateRequest
import com.back.devc.domain.post.comment.service.CommentService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("댓글/알림 통합 테스트")
internal class CommentNotificationIntegrationTest {
    @Autowired
    private lateinit var commentService: CommentService

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var postId = 0L
    private var postOwnerId = 0L
    private var commentWriterId = 0L

    @BeforeEach
    fun setUp() {
        postOwnerId = 1L
        commentWriterId = 2L
        postId = 1L

        // H2 테스트 DB 실제 테이블명 기준으로 테스트 데이터를 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS")
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS")
        jdbcTemplate.update("DELETE FROM COMMENTS")
        jdbcTemplate.update("DELETE FROM POST_LIKES")
        jdbcTemplate.update("DELETE FROM BOOKMARKS")
        jdbcTemplate.update("DELETE FROM REPORTS")
        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM USERS")

        jdbcTemplate.update(
            """
                INSERT INTO USERS (
                    USER_ID,
                    EMAIL,
                    NICKNAME,
                    PASSWORD_HASH,
                    PROVIDER,
                    PROVIDER_USER_ID,
                    ROLE,
                    STATUS,
                    SUSPENDED_UNTIL,
                    CREATED_AT,
                    UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                
                """.trimIndent(),
            postOwnerId,
            "post-owner@test.com",
            "게시글작성자",
            "$2a$10\$testPasswordHashForIntegrationTestOwner",
            "LOCAL",
            "post-owner@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        jdbcTemplate.update(
            """
                INSERT INTO USERS (
                    USER_ID,
                    EMAIL,
                    NICKNAME,
                    PASSWORD_HASH,
                    PROVIDER,
                    PROVIDER_USER_ID,
                    ROLE,
                    STATUS,
                    SUSPENDED_UNTIL,
                    CREATED_AT,
                    UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                
                """.trimIndent(),
            commentWriterId,
            "comment-writer@test.com",
            "댓글작성자",
            "$2a$10\$testPasswordHashForIntegrationTestWriter",
            "LOCAL",
            "comment-writer@test.com",
            "USER",
            "ACTIVE",
            null,
        )

        jdbcTemplate.update(
            """
                INSERT INTO POST (
                    POST_ID,
                    USER_ID,
                    TITLE,
                    CONTENT,
                    CATEGORY_ID,
                    IS_DELETED,
                    LIKE_COUNT,
                    COMMENT_COUNT,
                    VIEW_COUNT,
                    CREATED_AT,
                    UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                
                """.trimIndent(),
            postId,
            postOwnerId,
            "통합 테스트 게시글",
            "통합 테스트 게시글 내용",
            1L,
            false,
            0,
            0,
            0,
        )
    }

    @Test
    @DisplayName("댓글 작성 후 트랜잭션 커밋 이후 댓글 알림이 생성되고 읽음 처리된다")
    fun createComment_thenCreateNotification_afterCommit_andReadNotification() {
        // given
        val request = CommentCreateRequest("통합 테스트 댓글입니다.")

        // when
        val commentResponse = commentService.createComment(postId, commentWriterId, request)

        // @TransactionalEventListener(AFTER_COMMIT)로 알림이 생성되므로 테스트 트랜잭션을 커밋한다.
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // then
        val notificationListResponse = notificationService.getMyNotifications(postOwnerId, 0, 20, "comments")

        val createdNotification = notificationListResponse.notifications
            .firstOrNull { notification ->
                notification.type == "COMMENT" && notification.commentId == commentResponse.commentId
            } ?: throw AssertionError("댓글 작성 후 생성된 댓글 알림을 찾을 수 없습니다.")

        Assertions.assertThat(createdNotification.actorUserId).isEqualTo(commentWriterId)
        Assertions.assertThat(createdNotification.postId).isEqualTo(postId)
        Assertions.assertThat(createdNotification.commentId).isEqualTo(commentResponse.commentId)
        Assertions.assertThat(createdNotification.isRead).isFalse()

        val readNotification = notificationService.readNotification(
            createdNotification.notificationId,
            postOwnerId,
        )

        Assertions.assertThat(readNotification.isRead).isTrue()
    }
}