package com.back.devc.domain.interaction.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public NotificationListResponse(List<NotificationResponse> notifications) {
        this(notifications, 0, notifications.size(), notifications.size(), 1, false);
    }
}