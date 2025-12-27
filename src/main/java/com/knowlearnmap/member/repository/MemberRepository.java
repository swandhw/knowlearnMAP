package com.knowlearnmap.member.repository;

import com.knowlearnmap.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByVerificationToken(String verificationToken);

    boolean existsByEmail(String email);

    java.util.List<Member> findByDomain(String domain);
}
