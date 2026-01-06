package com.knowlearnmap.member.controller;

import com.knowlearnmap.member.domain.UpgradeRequest;
import com.knowlearnmap.member.repository.UpgradeRequestRepository;
import com.knowlearnmap.member.repository.MemberRepository;
import com.knowlearnmap.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/upgrade")
@RequiredArgsConstructor
@Slf4j
public class UpgradeRequestController {

    private final UpgradeRequestRepository upgradeRequestRepository;
    private final MemberRepository memberRepository;
    private final String UPLOAD_DIR = "uploads/upgrade_docs/";

    @PostMapping("/request")
    public ResponseEntity<String> submitRequest(
            @RequestParam("type") String typeStr,
            @RequestParam("company") String company,
            @RequestParam("name") String name,
            @RequestParam("phone") String phone,
            @RequestParam(value = "meetingTime", required = false) String meetingTimeStr,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            jakarta.servlet.http.HttpServletRequest httpRequest) { // Add HttpServletRequest

        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();

        log.info("Upgrade Request Auth Check: {}", authentication);
        if (httpRequest.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : httpRequest.getCookies()) {
                log.info("Cookie: {}={}", c.getName(), c.getValue());
            }
        } else {
            log.info("No cookies found in request.");
        }

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            log.warn("Upgrade Request Unauthorized: Auth is " + authentication);
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + email));

        try {
            UpgradeRequest.RequestType type = UpgradeRequest.RequestType.valueOf(typeStr);

            // Save files
            List<String> savedPaths = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (MultipartFile file : files) {
                    if (file.isEmpty())
                        continue;
                    String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                    // Simple unique naming
                    String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    savedPaths.add(filePath.toString());
                }
            }

            String documentPaths = String.join(",", savedPaths);

            LocalDateTime meetingTime = null;
            if (meetingTimeStr != null && !meetingTimeStr.isBlank()) {
                meetingTime = LocalDateTime.parse(meetingTimeStr);
            }

            UpgradeRequest request = new UpgradeRequest(
                    member.getId(),
                    type,
                    company,
                    name,
                    phone,
                    documentPaths,
                    meetingTime);

            upgradeRequestRepository.save(request);

            return ResponseEntity.ok("Upgrade request submitted successfully.");

        } catch (Exception e) {
            log.error("Failed to process upgrade request", e);
            return ResponseEntity.status(500).body("Failed to process request: " + e.getMessage());
        }
    }
}
