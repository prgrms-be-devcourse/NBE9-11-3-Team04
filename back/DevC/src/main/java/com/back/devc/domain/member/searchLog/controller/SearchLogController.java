package com.back.devc.domain.member.searchLog.controller;

import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest;
import com.back.devc.domain.member.searchLog.dto.PopularKeywordResponse;
import com.back.devc.domain.member.searchLog.dto.SearchLogResponse;
import com.back.devc.domain.member.searchLog.service.SearchLogService;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchLogController {

    private final SearchLogService searchLogService;

    @PostMapping("/search-logs")
    public SearchLogResponse createSearchLog(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody CreateSearchLogRequest request
    ) {
        return searchLogService.createSearchLog(principal.userId(), request);
    }

    @GetMapping("/users/me/search-logs")
    public List<SearchLogResponse> getMySearchLogs(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return searchLogService.getMySearchLogs(principal.userId());
    }

    @DeleteMapping("/users/me/search-logs/{searchLogId}")
    public void deleteSearchLog(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long searchLogId
    ) {
        searchLogService.deleteSearchLog(principal.userId(), searchLogId);
    }

    @DeleteMapping("/users/me/search-logs")
    public void deleteAllSearchLogs(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        searchLogService.deleteAllSearchLogs(principal.userId());
    }

    @GetMapping("/search-logs/popular")
    public List<PopularKeywordResponse> getPopularKeywords() {
        return searchLogService.getPopularKeywords();
    }
}