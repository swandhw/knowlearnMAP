package com.knowlearnmap.domain.controller;

import com.knowlearnmap.domain.domain.DomainEntity;
import com.knowlearnmap.domain.service.DomainService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;

    @GetMapping
    public ResponseEntity<List<DomainEntity>> getAllDomains() {
        return ResponseEntity.ok(domainService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createDomain(@RequestBody CreateDomainRequest request) {
        try {
            DomainEntity created = domainService.createDomain(
                    request.getName(),
                    request.getDescription(),
                    request.getArangoDbName());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class CreateDomainRequest {
        private String name;
        private String description;
        private String arangoDbName;
    }
}
