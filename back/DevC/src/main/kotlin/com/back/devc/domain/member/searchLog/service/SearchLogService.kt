package com.back.devc.domain.member.searchLog.service

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest
import com.back.devc.domain.member.searchLog.dto.PopularKeywordResponse
import com.back.devc.domain.member.searchLog.dto.SearchLogResponse
import com.back.devc.domain.member.searchLog.entity.SearchLog
import com.back.devc.domain.member.searchLog.repository.SearchLogRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class SearchLogService(
    private val searchLogRepository: SearchLogRepository,
    private val memberRepository: MemberRepository,
) {

    @Transactional
    fun createSearchLog(
        userId: Long,
        request: CreateSearchLogRequest,
    ): SearchLogResponse {
        val member = findMemberById(userId)

        val searchLog = SearchLog(
            member = member,
            keyword = request.keyword,
        )

        val saved = searchLogRepository.save(searchLog)

        return SearchLogResponse(
            searchLogId = saved.searchLogId
                ?: throw IllegalStateException("검색 기록 ID가 생성되지 않았습니다."),
            keyword = saved.keyword,
            searchedAt = saved.searchedAt,
        )
    }

    fun getMySearchLogs(userId: Long): List<SearchLogResponse> {
        val member = findMemberById(userId)

        return searchLogRepository.findAllByMemberOrderBySearchedAtDesc(member)
            .map { searchLog ->
                SearchLogResponse(
                    searchLogId = searchLog.searchLogId
                        ?: throw IllegalStateException("검색 기록 ID가 존재하지 않습니다."),
                    keyword = searchLog.keyword,
                    searchedAt = searchLog.searchedAt,
                )
            }
    }

    @Transactional
    fun deleteSearchLog(
        userId: Long,
        searchLogId: Long,
    ) {
        val member = findMemberById(userId)

        val searchLog = searchLogRepository.findBySearchLogIdAndMember(
            searchLogId,
            member,
        ).orElseThrow {
            EntityNotFoundException("검색 기록을 찾을 수 없습니다. id=$searchLogId")
        }

        searchLogRepository.delete(searchLog)
    }

    @Transactional
    fun deleteAllSearchLogs(userId: Long) {
        val member = findMemberById(userId)

        searchLogRepository.deleteAllByMember(member)
    }

    fun getPopularKeywords(): List<PopularKeywordResponse> {
        val from = LocalDateTime.now().minusDays(7)

        return searchLogRepository.findPopularKeywordsSince(from)
            .map { result ->
                PopularKeywordResponse(
                    keyword = result[0] as String,
                    count = (result[1] as Number).toLong(),
                )
            }
    }

    private fun findMemberById(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                EntityNotFoundException("회원을 찾을 수 없습니다. id=$userId")
            }
    }
}