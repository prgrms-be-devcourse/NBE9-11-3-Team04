package com.back.devc.domain.interaction.notification.type

enum class NotificationType(
    val value: String,
) {
    COMMENT("COMMENT"),
    REPLY("REPLY"),
    LIKE("LIKE"),
    REPORT("REPORT"),
}