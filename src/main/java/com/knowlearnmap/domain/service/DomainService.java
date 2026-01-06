package com.knowlearnmap.domain.service;

import com.knowlearnmap.domain.domain.DomainEntity;
import com.knowlearnmap.domain.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DomainService {

    private final DomainRepository domainRepository;
    private final com.arangodb.ArangoDB arangoDB;

    public List<DomainEntity> findAll() {
        return domainRepository.findAll();
    }

    public java.util.Optional<DomainEntity> findByName(String name) {
        return domainRepository.findByName(name);
    }

    @Transactional
    public DomainEntity createDomain(String name, String description, String arangoDbName) {
        // Validation
        if (domainRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 도메인 이름입니다: " + name);
        }
        if (domainRepository.existsByArangoDbName(arangoDbName)) {
            throw new IllegalArgumentException("이미 사용 중인 ArangoDB 이름입니다: " + arangoDbName);
        }
        // Relaxed regex: allow lowercase, numbers, underscores, hyphens
        if (!arangoDbName.matches("^[a-z0-9_-]+$")) {
            throw new IllegalArgumentException("ArangoDB 이름은 소문자, 숫자, 하이픈, 언더스코어만 허용됩니다.");
        }

        DomainEntity domain = DomainEntity.builder()
                .name(name)
                .description(description)
                .arangoDbName(arangoDbName)
                .isActive(true)
                .createdId("admin") // TODO: get from security context
                .updatedId("admin")
                .createdDatetime(LocalDateTime.now())
                .updatedDatetime(LocalDateTime.now())
                .build();

        DomainEntity savedDomain = domainRepository.save(domain);

        // Provision ArangoDB Database
        try {
            if (!arangoDB.db(arangoDbName).exists()) {
                arangoDB.createDatabase(arangoDbName);
                // Ensure default collections exist? (Optional, OntologyToArangoService handles
                // sync/creation usually)
                // But for a fresh start, just creating DB is enough.
            }
        } catch (Exception e) {
            // Log error but don't fail transaction? Or fail?
            // Better to fail so we don't end up with desync.
            throw new RuntimeException("Failed to create ArangoDB database: " + arangoDbName, e);
        }

        return savedDomain;
    }
}
