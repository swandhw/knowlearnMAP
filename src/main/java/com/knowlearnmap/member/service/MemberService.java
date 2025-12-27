package com.knowlearnmap.member.service;

import com.knowlearnmap.member.domain.Member;
import com.knowlearnmap.member.dto.SignupRequest;
import com.knowlearnmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.knowlearnmap.common.service.MailService mailService;

    @Transactional
    public Long signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String token = java.util.UUID.randomUUID().toString();
        // Initial password can be random or placeholder since it will be reset later
        // But for now we store the requested password or just a temp one?
        // User request: "1.2 ... 비밀번호 생성(입력) 하도록". This implies the user DOES NOT set
        // password at signup.
        // So we will set a random dummy password initially.
        String tempPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());

        Member member = new Member(
                request.getEmail(),
                tempPassword,
                Member.Role.SYSOP, // Default signup is now for SYSOP candidates (who will create Users later)
                token,
                null // Domain set later or requested? For now null.
        );

        memberRepository.save(member);

        // Send Verification Email
        // Assuming frontend URL is http://localhost:5173
        String verificationLink = "http://localhost:5173/verify-email?token=" + token;
        mailService.sendEmail(request.getEmail(), "[KnowlearnMAP] Email Verification",
                "Please click the link to verify your email: " + verificationLink);

        return member.getId();
    }

    @Transactional
    public void verifyEmail(String token) {
        Member member = memberRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (member.getStatus() != Member.Status.VERIFYING_EMAIL) {
            throw new IllegalArgumentException("Invalid status for verification");
        }

        member.setStatus(Member.Status.WAITING_APPROVAL);
        member.setVerificationToken(null); // Clear token

        // Notify Admin (Optional, could send email to admin here)
        log.info("Member {} verified email. Waiting for approval.", member.getEmail());
    }

    @Transactional
    public void approveMember(Long memberId, String domain) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getStatus() != Member.Status.WAITING_APPROVAL) {
            throw new IllegalArgumentException("Member is not waiting for approval");
        }

        String token = java.util.UUID.randomUUID().toString();
        member.setStatus(Member.Status.APPROVED_WAITING_PASSWORD);
        member.setVerificationToken(token);

        // Admin assigns domain to the Sysop/User
        if (domain != null && !domain.isBlank()) {
            member.setDomain(domain);
        }

        // Send Set Password Email
        String setPasswordLink = "http://localhost:5173/set-password?token=" + token;
        mailService.sendEmail(member.getEmail(), "[KnowlearnMAP] Account Approved - Set Password",
                "Your account has been approved. Please set your password: " + setPasswordLink);
    }

    @Transactional
    public void createUser(String email, String domain) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String token = java.util.UUID.randomUUID().toString();
        // Random temp password
        String tempPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());

        Member member = new Member(
                email,
                tempPassword,
                Member.Role.USER, // Created by Sysop -> Role USER
                token,
                domain // Inherited from Sysop
        );
        member.setStatus(Member.Status.APPROVED_WAITING_PASSWORD); // Skip verification, go straight to set password

        memberRepository.save(member);

        // Send Set Password Email (Simulated "Welcome" email for User)
        String setPasswordLink = "http://localhost:5173/set-password?token=" + token;
        mailService.sendEmail(email, "[KnowlearnMAP] Invitation",
                "You have been invited to join. Please set your password: " + setPasswordLink);
    }

    public java.util.List<Member> getMembersByDomain(String domain) {
        return memberRepository.findByDomain(domain);
    }

    @Transactional
    public void setPassword(String token, String newPassword) {
        Member member = memberRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (member.getStatus() != Member.Status.APPROVED_WAITING_PASSWORD) {
            throw new IllegalArgumentException("Invalid status for setting password");
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        member.setStatus(Member.Status.ACTIVE);
        member.setVerificationToken(null);
    }
}
