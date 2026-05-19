package com.back.devc.domain.interaction.report.repository

interface ReportReasonStatProjection {
    val reportGroupId: Long
    val reasonType: String
    val reasonCount: Long
}