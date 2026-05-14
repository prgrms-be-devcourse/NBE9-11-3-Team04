package com.back.devc.domain.interaction.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications
) {
}