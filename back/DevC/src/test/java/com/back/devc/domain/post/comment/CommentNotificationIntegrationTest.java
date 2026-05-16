package com.back.devc.domain.post.comment;

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse;
import com.back.devc.domain.interaction.notification.dto.NotificationResponse;
import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.post.comment.dto.CommentCreateRequest;
import com.back.devc.domain.post.comment.dto.CommentResponse;
import com.back.devc.domain.post.comment.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("댓글/알림 통합 테스트")
class CommentNotificationIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long postId;
    private Long postOwnerId;
    private Long commentWriterId;

    @BeforeEach
    void setUp() {
        postOwnerId = 1L;
        commentWriterId = 2L;
        postId = 1L;

        // H2 테스트 DB 실제 테이블명 기준으로 테스트 데이터를 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS");
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS");
        jdbcTemplate.update("DELETE FROM COMMENTS");
        jdbcTemplate.update("DELETE FROM POST_LIKES");
        jdbcTemplate.update("DELETE FROM BOOKMARKS");
        jdbcTemplate.update("DELETE FROM REPORTS");
        jdbcTemplate.update("DELETE FROM POST");
        jdbcTemplate.update("DELETE FROM USERS");

        jdbcTemplate.update("""
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
                """,
                postOwnerId,
                "post-owner@test.com",
                "게시글작성자",
                "$2a$10$testPasswordHashForIntegrationTestOwner",
                "LOCAL",
                "post-owner@test.com",
                "USER",
                "ACTIVE",
                null
        );

        jdbcTemplate.update("""
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
                """,
                commentWriterId,
                "comment-writer@test.com",
                "댓글작성자",
                "$2a$10$testPasswordHashForIntegrationTestWriter",
                "LOCAL",
                "comment-writer@test.com",
                "USER",
                "ACTIVE",
                null
        );

        jdbcTemplate.update("""
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
                """,
                postId,
                postOwnerId,
                "통합 테스트 게시글",
                "통합 테스트 게시글 내용",
                1L,
                false,
                0,
                0,
                0
        );
    }

    @Test
    @DisplayName("댓글 작성 후 트랜잭션 커밋 이후 댓글 알림이 생성되고 읽음 처리된다")
    void createComment_thenCreateNotification_afterCommit_andReadNotification() {
        // given
        CommentCreateRequest request = new CommentCreateRequest("통합 테스트 댓글입니다.");

        // when
        CommentResponse commentResponse = commentService.createComment(postId, commentWriterId, request);

        // @TransactionalEventListener(AFTER_COMMIT)로 알림이 생성되므로 테스트 트랜잭션을 커밋한다.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // then
        NotificationListResponse notificationListResponse = notificationService.getMyNotifications(postOwnerId, 0, 20, "comments");

        NotificationResponse createdNotification = notificationListResponse.notifications().stream()
                .filter(notification -> "COMMENT".equals(notification.type()))
                .filter(notification -> notification.commentId().equals(commentResponse.getCommentId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("댓글 작성 후 생성된 댓글 알림을 찾을 수 없습니다."));

        assertThat(createdNotification.actorUserId()).isEqualTo(commentWriterId);
        assertThat(createdNotification.postId()).isEqualTo(postId);
        assertThat(createdNotification.commentId()).isEqualTo(commentResponse.getCommentId());
        assertThat(createdNotification.isRead()).isFalse();

        NotificationResponse readNotification = notificationService.readNotification(
                createdNotification.notificationId(),
                postOwnerId
        );

        assertThat(readNotification.isRead()).isTrue();
    }
}
