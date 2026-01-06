package com.knowlearnmap.member.controller;

import com.knowlearnmap.member.domain.Member;
import com.knowlearnmap.member.domain.UpgradeRequest;
import com.knowlearnmap.member.repository.MemberRepository;
import com.knowlearnmap.member.repository.UpgradeRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/upgrades")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminUpgradeController {

    private final UpgradeRequestRepository upgradeRequestRepository;
    private final MemberRepository memberRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllRequests() {
        List<UpgradeRequest> requests = upgradeRequestRepository.findAll();

        // Enrich with member email for display
        List<Map<String, Object>> response = requests.stream().map(req -> {
            String email = memberRepository.findById(req.getMemberId())
                    .map(Member::getEmail)
                    .orElse("Unknown");

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", req.getId());
            map.put("memberId", req.getMemberId());
            map.put("email", email);
            map.put("type", req.getRequestType());
            map.put("company", req.getCompanyName());
            map.put("name", req.getContactName());
            map.put("phone", req.getPhone());
            map.put("status", req.getStatus());
            map.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().toString() : "");
            map.put("meetingTime", req.getMeetingDateTime() != null ? req.getMeetingDateTime().toString() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        UpgradeRequest request = upgradeRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() == UpgradeRequest.RequestStatus.APPROVED) {
            return ResponseEntity.badRequest().body("Request already approved");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // Update Member Grade
        if (request.getRequestType() == UpgradeRequest.RequestType.PRO_UPGRADE) {
            member.setGrade(Member.Grade.PRO);
        } else if (request.getRequestType() == UpgradeRequest.RequestType.MAX_CONSULTATION) {
            member.setGrade(Member.Grade.MAX);
        }

        // Update Request Status
        request.setStatus(UpgradeRequest.RequestStatus.APPROVED);

        memberRepository.save(member); // Explicit save to persist grade if needed (JPA txn should handle it)
        upgradeRequestRepository.save(request);

        return ResponseEntity.ok("Approved successfully. Member grade updated to " + member.getGrade());
    }
}
