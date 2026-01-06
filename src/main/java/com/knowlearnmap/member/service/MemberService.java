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
    private final com.knowlearnmap.domain.service.DomainService domainService;

    @Transactional
    public Long signup(SignupRequest request) {
        java.util.Optional<Member> existingMember = memberRepository.findByEmail(request.getEmail());

        Member member;
        String token = java.util.UUID.randomUUID().toString();

        if (existingMember.isPresent()) {
            member = existingMember.get();
            if (member.getStatus() == Member.Status.VERIFYING_EMAIL) {
                // Reuse existing member - reset token and expiry
                log.info("Reuse existing member for signup: {}", request.getEmail());
                member.setVerificationToken(token);
                member.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));
                // Resend email below
            } else {
                // Already active or verified
                throw new IllegalArgumentException("Email already exists");
            }
        } else {
            // New Member
            String tempPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
            member = new Member(
                    request.getEmail(),
                    tempPassword,
                    Member.Role.SYSOP,
                    token,
                    null);
            member.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));
            memberRepository.save(member);
        }

        // Send Verification Email (HTML)
        String verificationLink = "http://localhost:5173/verify-email?token=" + token;

        // Option A Copy + Provided HTML Template
        String subject = "[KnowlearnMAP] 가입을 위해 이메일 주소를 인증해 주세요.";
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #f4f7f9; font-family: 'Pretendard', 'Apple SD Gothic Neo', sans-serif;\">\n"
                +
                "    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"margin-top: 50px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);\">\n"
                +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 40px 0 20px 0; background-color: #2563eb;\">\n" +
                "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: -0.5px;\">Welcome to KnowlearnMAP</h1>\n"
                +
                "            </td>\n" +
                "        </tr>\n" +
                "        \n" +
                "        <tr>\n" +
                "            <td style=\"padding: 40px 30px;\">\n" +
                "                <h2 style=\"color: #1f2937; margin-top: 0; font-size: 20px;\">가입을 진심으로 환영합니다!</h2>\n" +
                "                <p style=\"color: #4b5563; line-height: 1.6; font-size: 15px;\">\n" +
                "                    KnowlearnMAP의 회원이 되신 것을 축하드립니다.<br>\n" +
                "                    아래 버튼을 클릭하여 이메일 주소 인증을 완료하고 서비스를 시작해 보세요.\n" +
                "                </p>\n" +
                "                \n" +
                "                <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 30px 0;\">\n"
                +
                "                    <tr>\n" +
                "                        <td align=\"center\" bgcolor=\"#2563eb\" style=\"border-radius: 6px;\">\n" +
                "                            <a href=\"" + verificationLink
                + "\" target=\"_blank\" style=\"display: inline-block; padding: 14px 40px; color: #ffffff; text-decoration: none; font-weight: 600; font-size: 16px;\">이메일 인증하기</a>\n"
                +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "                \n" +
                "                <p style=\"color: #9ca3af; font-size: 13px; line-height: 1.5;\">\n" +
                "                    * 이 링크는 보안을 위해 <strong>24시간 동안만 유효</strong>합니다.<br>\n" +
                "                    * 본인이 가입한 적이 없다면 이 메일을 무시해 주세요.<br>\n" +
                "                    * 버튼이 작동하지 않는다면 다음 주소를 주소창에 복사해 주세요: <br>" + verificationLink + "\n" +
                "                </p>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "        \n" +
                "        <tr>\n" +
                "            <td style=\"padding: 20px 30px; background-color: #f9fafb; border-top: 1px solid #f3f4f6; text-align: center;\">\n"
                +
                "                <p style=\"color: #9ca3af; font-size: 12px; margin: 0;\">\n" +
                "                    &copy; 2024 KnowlearnMAP. All rights reserved.<br>\n" +
                "                    Seoul, South Korea\n" +
                "                </p>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";

        mailService.sendHtmlEmail(request.getEmail(), subject, htmlContent);

        return member.getId();
    }

    @Transactional
    public void verifyEmail(String token) {
        Member member = memberRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (member.getStatus() != Member.Status.VERIFYING_EMAIL) {
            // Idempotency: If already verified and waiting for password, consider it a
            // success
            if (member.getStatus() == Member.Status.APPROVED_WAITING_PASSWORD) {
                log.info("Member {} is already verified (APPROVED_WAITING_PASSWORD). Returning success.",
                        member.getEmail());
                return;
            }
            throw new IllegalArgumentException("Invalid status for verification");
        }

        // Check Expiry
        if (member.getVerificationTokenExpiry() != null
                && java.time.LocalDateTime.now().isAfter(member.getVerificationTokenExpiry())) {
            throw new IllegalArgumentException("Verification token has expired. Please sign up again.");
            // Note: In a real system, we might delete the member or allow resending email.
        }

        // Update Status
        member.setStatus(Member.Status.APPROVED_WAITING_PASSWORD);

        // DO NOT Generate new token - keep existing token for setPassword
        // String setPasswordToken = java.util.UUID.randomUUID().toString();
        // member.setVerificationToken(setPasswordToken);
        // member.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));

        log.info("Member {} verified email. Waiting for password setup.", member.getEmail());
    }

    // Reuse existing for Admin/Legacy if needed, but signup flow uses above.
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
        // Also expiry
        member.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));

        if (domain != null && !domain.isBlank()) {
            member.setDomain(domain);
        }

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

        // Auto Provisioning Domain (Moved from verifyEmail)
        String email = member.getEmail();

        // Rule: user.name@company.com -> db-user_name-company_com
        String safeName = email.replace("@", "-");
        safeName = safeName.replace(".", "_");
        safeName = safeName.replaceAll("[^a-zA-Z0-9_\\-]", "");

        String dbName = "db-" + safeName;
        String domainName = email;

        try {
            // Check if domain already exists (Idempotency)
            java.util.Optional<com.knowlearnmap.domain.domain.DomainEntity> existingDomain = domainService
                    .findByName(domainName);
            if (existingDomain.isPresent()) {
                log.info("Domain {} already exists. Reusing it.", domainName);
            } else {
                domainService.createDomain(domainName, "Auto-generated for " + email, dbName);
            }
            member.setDomain(domainName);
        } catch (Exception e) {
            log.error("Failed to provision domain for {}", email, e);
            throw new RuntimeException("Domain provisioning failed: " + e.getMessage());
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        member.setStatus(Member.Status.ACTIVE);
        member.setVerificationToken(null);
    }

    public java.util.Optional<Member> getMember(String email) {
        return memberRepository.findByEmail(email);
    }

    public void sendTestEmail(String to) {
        mailService.sendEmail(to, "[KnowlearnMAP] Test Email",
                "This is a test email from KnowlearnMAP. If you receive this, email configuration is working correctly!");
        log.info("Test email sent to: {}", to);
    }
}
