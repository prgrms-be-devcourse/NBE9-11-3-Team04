package com.back.devc.domain.interaction.notification.dto

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    constructor(notifications: List<NotificationResponse>) : this(
        notifications = notifications,
        page = 0,
        size = notifications.size,
        totalElements = notifications.size.toLong(),
        totalPages = 1,
        hasNext = false,
    )
}