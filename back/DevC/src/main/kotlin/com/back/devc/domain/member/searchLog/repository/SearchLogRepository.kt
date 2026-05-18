package com.back.devc.domain.member.searchLog.repository

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.searchLog.entity.SearchLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

interface SearchLogRepository : JpaRepository<SearchLog, Long> {

    fun findAllByMemberOrderBySearchedAtDesc(
        member: Member,
    ): List<SearchLog>

    fun findBySearchLogIdAndMember(
        searchLogId: Long,
        member: Member,
    ): Optional<SearchLog>

    @Modifying
    fun deleteAllByMember(
        member: Member,
    )

    @Query(
        """
            select s.keyword, count(s)
            from SearchLog s
            where s.searchedAt >= :from
            group by s.keyword
            order by count(s) desc
        """
    )
    fun findPopularKeywordsSince(
        from: LocalDateTime,
    ): List<Array<Any>>
}