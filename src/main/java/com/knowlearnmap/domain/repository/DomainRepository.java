package com.knowlearnmap.domain.repository;

import com.knowlearnmap.domain.domain.DomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<DomainEntity, Long> {
}
