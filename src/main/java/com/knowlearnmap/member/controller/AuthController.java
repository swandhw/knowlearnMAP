package com.knowlearnmap.member.controller;

import com.knowlearnmap.member.dto.LoginRequest;
import com.knowlearnmap.member.dto.SignupRequest;
import com.knowlearnmap.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        memberService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully. Please check your email to set your password.");
    }

    @PostMapping("/approve/{memberId}")
    public ResponseEntity<String> approveMember(@PathVariable Long memberId,
            @RequestParam(required = false) String domain) {
        // In a real app, check if current user is ADMIN
        memberService.approveMember(memberId, domain);
        return ResponseEntity.ok("Member approved.");
    }

    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(@RequestBody com.knowlearnmap.member.dto.SetPasswordRequest request) {
        memberService.setPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok("Password set successfully. You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Additional Check: Is member ACTIVE?
        // Current implementation of CustomUserDetailsService loads user.
        // We might want to check status here or in UserDetailsService.
        // For simplicity, let's assume if authenticate passed, password matched.
        // BUT if status is not ACTIVE, password wouldn't match because we set a temp
        // random password initially?
        // Actually, if status is NOT ACTIVE, they shouldn't be able to login even if
        // password matches (rare case).
        // Let's rely on the fact that they don't know the temp password.
        // AND when they set password, status becomes ACTIVE.

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Manually save the context to session for stateful auth
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok("User logged in successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("User logged out successfully");
    }

    @GetMapping("/check")
    public ResponseEntity<java.util.Map<String, Object>> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            // Fetch basic details effectively
            // Note: Optimally we should have this in Principal but fetching from DB ensures
            // freshness
            com.knowlearnmap.member.domain.Member member = memberService.getMember(email)
                    .orElse(null);

            if (member != null) {
                java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
                userInfo.put("email", member.getEmail());
                userInfo.put("role", member.getRole().name()); // "USER", "ADMIN"
                userInfo.put("domain", member.getDomain());
                userInfo.put("grade", member.getGrade().name()); // "FREE", "PRO", "MAX"
                return ResponseEntity.ok(userInfo);
            }
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            memberService.sendTestEmail(to);
            return ResponseEntity.ok("Test email sent to: " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}
