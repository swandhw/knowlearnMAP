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

    public List<DomainEntity> findAll() {
        return domainRepository.findAll();
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
        if (!arangoDbName.matches("^[a-z]+$")) {
            throw new IllegalArgumentException("ArangoDB 이름은 소문자 알파벳만 허용됩니다.");
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

        return domainRepository.save(domain);
    }
}
