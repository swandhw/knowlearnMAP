package com.knowlearnmap.domain.repository;

import com.knowlearnmap.domain.domain.DomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DomainRepository extends JpaRepository<DomainEntity, Long> {
    Optional<DomainEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByArangoDbName(String arangoDbName);
}
