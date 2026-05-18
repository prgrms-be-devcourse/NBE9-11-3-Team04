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
    // Java record accessor 호환을 위해 임시 유지
    fun content(): List<T> = content
    fun page(): Int = page
    fun size(): Int = size
    fun totalElements(): Long = totalElements
    fun totalPages(): Int = totalPages
    fun first(): Boolean = first
    fun last(): Boolean = last
    fun hasNext(): Boolean = hasNext

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