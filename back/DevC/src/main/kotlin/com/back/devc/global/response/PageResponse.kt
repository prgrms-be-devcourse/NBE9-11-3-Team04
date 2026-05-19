package com.back.devc.global.response

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val hasNext: Boolean
) {

    companion object {
        @JvmStatic
        fun <T : Any> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.getContent(),
                page = page.getNumber(),
                size = page.getSize(),
                totalElements = page.getTotalElements(),
                totalPages = page.getTotalPages(),
                first = page.isFirst(),
                last = page.isLast(),
                hasNext = page.hasNext()
            )
        }
    }
}