package com.knowlearnmap.config;

import com.knowlearnmap.member.domain.Member;
import com.knowlearnmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MemberDataInitializer {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            createMemberIfNotFound("user", "test", Member.Role.USER, "dplab");
            createMemberIfNotFound("sysop", "test", Member.Role.SYSOP, "dplab");
            createMemberIfNotFound("admin", "test", Member.Role.ADMIN, null);
        };
    }

    private void createMemberIfNotFound(String email, String password, Member.Role role, String domain) {
        if (!memberRepository.existsByEmail(email)) {
            Member member = new Member(
                    email,
                    passwordEncoder.encode(password),
                    role,
                    null, // no verification token needed
                    domain);
            // Force status to ACTIVE
            member.setStatus(Member.Status.ACTIVE);
            memberRepository.save(member);
            log.info("Created test account: {} / {} ({})", email, password, role);
        }
    }
}
