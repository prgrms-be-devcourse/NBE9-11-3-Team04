package com.back.devc.domain.member.member.repository;

import com.back.devc.domain.member.member.entity.AuthProvider;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    // 닉네임 검색 (부분 일치)
    Page<Member> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);

    Page<Member> findByNicknameContainingOrEmailContaining(
            String nickname,
            String email,
            Pageable pageable
    );

    Page<Member> findByStatusAndNicknameContainingOrStatusAndEmailContaining(
            MemberStatus status1,
            String nickname,
            MemberStatus status2,
            String email,
            Pageable pageable
    );

    List<Member> findAllByUserIdIn(List<Long> userIds);

}