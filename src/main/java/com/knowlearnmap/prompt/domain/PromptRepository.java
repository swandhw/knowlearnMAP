package com.knowlearnmap.prompt.domain;

import com.knowlearnmap.prompt.domain.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    Optional<Prompt> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT p FROM Prompt p WHERE " +
            "(:isActive IS NULL OR p.isActive = :isActive) AND " +
            "(:search IS NULL OR p.code LIKE %:search% OR p.name LIKE %:search%)")
    Page<Prompt> findByFilters(
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable);
}
