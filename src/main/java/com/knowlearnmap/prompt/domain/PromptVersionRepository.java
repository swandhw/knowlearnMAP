package com.knowlearnmap.prompt.domain;

import com.knowlearnmap.prompt.domain.PromptVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    List<PromptVersion> findByPromptCode(String promptCode);

    @Query("SELECT v FROM PromptVersion v WHERE " +
            "v.promptCode = :promptCode AND " +
            "(:status IS NULL OR v.status = :status) " +
            "ORDER BY v.version DESC")
    Page<PromptVersion> findByPromptCodeAndStatus(
            @Param("promptCode") String promptCode,
            @Param("status") String status,
            Pageable pageable);

    Optional<PromptVersion> findByPromptCodeAndVersion(String promptCode, Integer version);

    @Query("SELECT MAX(v.version) FROM PromptVersion v WHERE v.promptCode = :promptCode")
    Integer findMaxVersionByPromptCode(@Param("promptCode") String promptCode);

    Optional<PromptVersion> findByPromptCodeAndIsActive(String promptCode, Boolean isActive);

    @Query("SELECT COUNT(v) FROM PromptVersion v WHERE v.promptCode = :promptCode")
    Integer countByPromptCode(@Param("promptCode") String promptCode);

    @Query("SELECT COUNT(v) FROM PromptVersion v WHERE v.promptCode = :promptCode AND v.status = :status")
    Integer countByPromptCodeAndStatus(@Param("promptCode") String promptCode, @Param("status") String status);

    @Query("SELECT new com.knowlearnmap.prompt.dto.SatisfactionTrendResponse(v.version, v.avgSatisfaction, v.testCount) "
            +
            "FROM PromptVersion v WHERE v.promptCode = :promptCode AND v.status = 'published' " +
            "ORDER BY v.version DESC")
    List<com.knowlearnmap.prompt.dto.SatisfactionTrendResponse> findSatisfactionTrendByPromptCode(
            @Param("promptCode") String promptCode);
}
