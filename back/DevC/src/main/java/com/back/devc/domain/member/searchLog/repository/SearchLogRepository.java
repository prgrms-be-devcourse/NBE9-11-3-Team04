package com.back.devc.domain.member.searchLog.repository;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.searchLog.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findAllByMemberOrderBySearchedAtDesc(Member member);

    Optional<SearchLog> findBySearchLogIdAndMember(Long searchLogId, Member member);

    @Modifying
    void deleteAllByMember(Member member);

    @Query("""
            select s.keyword, count(s)
            from SearchLog s
            where s.searchedAt >= :from
            group by s.keyword
            order by count(s) desc
            """)
    List<Object[]> findPopularKeywordsSince(LocalDateTime from);
}