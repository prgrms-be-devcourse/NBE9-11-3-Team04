package com.back.devc.domain.member.searchLog.controller

import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest
import com.back.devc.domain.member.searchLog.dto.PopularKeywordResponse
import com.back.devc.domain.member.searchLog.dto.SearchLogResponse
import com.back.devc.domain.member.searchLog.service.SearchLogService
import com.back.devc.global.security.jwt.JwtPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class SearchLogController(
    private val searchLogService: SearchLogService,
) {

    @PostMapping("/search-logs")
    fun createSearchLog(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @RequestBody request: CreateSearchLogRequest,
    ): SearchLogResponse {
        return searchLogService.createSearchLog(
            principal.userId(),
            request,
        )
    }

    @GetMapping("/users/me/search-logs")
    fun getMySearchLogs(
        @AuthenticationPrincipal principal: JwtPrincipal,
    ): List<SearchLogResponse> {
        return searchLogService.getMySearchLogs(
            principal.userId(),
        )
    }

    @DeleteMapping("/users/me/search-logs/{searchLogId}")
    fun deleteSearchLog(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable searchLogId: Long,
    ) {
        searchLogService.deleteSearchLog(
            principal.userId(),
            searchLogId,
        )
    }

    @DeleteMapping("/users/me/search-logs")
    fun deleteAllSearchLogs(
        @AuthenticationPrincipal principal: JwtPrincipal,
    ) {
        searchLogService.deleteAllSearchLogs(
            principal.userId(),
        )
    }

    @GetMapping("/search-logs/popular")
    fun getPopularKeywords(): List<PopularKeywordResponse> {
        return searchLogService.getPopularKeywords()
    }
}