package com.knowlearnmap.member.controller;

import com.knowlearnmap.member.domain.Member;
import com.knowlearnmap.member.dto.SignupRequest;
import com.knowlearnmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final com.knowlearnmap.member.repository.MemberRepository memberRepository;

    @PostMapping
    @PreAuthorize("hasRole('SYSOP')") // Only Sysop (and Admin if hierarchy set)
    public ResponseEntity<String> createMember(@RequestBody SignupRequest request, Authentication authentication) {
        // Determine Domain
        String requesterEmail = authentication.getName();
        Member sysop = memberRepository.findByEmail(requesterEmail).orElseThrow();

        if (sysop.getRole() != Member.Role.SYSOP && sysop.getRole() != Member.Role.ADMIN) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        // Use Sysop's domain
        String domain = sysop.getDomain();

        memberService.createUser(request.getEmail(), domain);
        return ResponseEntity.ok("User created and invitation sent.");
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSOP')")
    public ResponseEntity<List<Member>> getMembers(Authentication authentication) {
        String requesterEmail = authentication.getName();
        Member sysop = memberRepository.findByEmail(requesterEmail).orElseThrow();

        // Return users in the same domain
        List<Member> members = memberService.getMembersByDomain(sysop.getDomain());
        return ResponseEntity.ok(members);
    }

    // TEST ONLY: Change user grade
    @PostMapping("/change-grade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeGrade(
            @RequestParam String username,
            @RequestParam String grade,
            Authentication authentication) {
        try {
            Member member = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Member.Grade newGrade = Member.Grade.valueOf(grade.toUpperCase());
            member.setGrade(newGrade);
            memberRepository.save(member);

            return ResponseEntity.ok("Grade changed to " + grade + " for user " + username);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
