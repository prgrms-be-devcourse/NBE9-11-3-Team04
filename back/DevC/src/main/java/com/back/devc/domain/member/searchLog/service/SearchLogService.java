package com.back.devc.domain.member.searchLog.service;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest;
import com.back.devc.domain.member.searchLog.dto.PopularKeywordResponse;
import com.back.devc.domain.member.searchLog.dto.SearchLogResponse;
import com.back.devc.domain.member.searchLog.entity.SearchLog;
import com.back.devc.domain.member.searchLog.repository.SearchLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SearchLogResponse createSearchLog(Long userId, CreateSearchLogRequest request) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. id=" + userId));

        SearchLog searchLog = new SearchLog(member, request.keyword());
        SearchLog saved = searchLogRepository.save(searchLog);

        return new SearchLogResponse(
                saved.getSearchLogId(),
                saved.getKeyword(),
                saved.getSearchedAt()
        );
    }

    public List<SearchLogResponse> getMySearchLogs(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. id=" + userId));

        return searchLogRepository.findAllByMemberOrderBySearchedAtDesc(member).stream()
                .map(searchLog -> new SearchLogResponse(
                        searchLog.getSearchLogId(),
                        searchLog.getKeyword(),
                        searchLog.getSearchedAt()
                ))
                .toList();
    }

    @Transactional
    public void deleteSearchLog(Long userId, Long searchLogId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. id=" + userId));

        SearchLog searchLog = searchLogRepository.findBySearchLogIdAndMember(searchLogId, member)
                .orElseThrow(() -> new EntityNotFoundException("검색 기록을 찾을 수 없습니다. id=" + searchLogId));

        searchLogRepository.delete(searchLog);
    }

    @Transactional
    public void deleteAllSearchLogs(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. id=" + userId));

        searchLogRepository.deleteAllByMember(member);
    }

    public List<PopularKeywordResponse> getPopularKeywords() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);

        return searchLogRepository.findPopularKeywordsSince(from).stream()
                .map(result -> new PopularKeywordResponse(
                        (String) result[0],
                        (Long) result[1]
                ))
                .toList();
    }
}