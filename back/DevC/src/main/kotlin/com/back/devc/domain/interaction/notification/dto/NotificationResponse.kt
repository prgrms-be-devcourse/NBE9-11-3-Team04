package com.back.devc.domain.interaction.notification.dto

import java.time.LocalDateTime

data class NotificationResponse(
    val notificationId: Long,
    val userId: Long,
    val actorUserId: Long,
    val actorNickname: String,
    val postId: Long?,
    val commentId: Long?,
    val type: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
)