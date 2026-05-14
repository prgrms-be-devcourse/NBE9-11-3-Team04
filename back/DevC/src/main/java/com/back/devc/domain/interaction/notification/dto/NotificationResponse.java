package com.back.devc.domain.interaction.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long userId,
        Long actorUserId,
        String actorNickname,
        Long postId,
        Long commentId,
        String type,
        String message,
        boolean isRead,
        LocalDateTime createdAt
) {
}