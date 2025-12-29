package com.knowlearnmap.prompt.domain;

import com.knowlearnmap.prompt.domain.PromptTestSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromptTestSnapshotRepository extends JpaRepository<PromptTestSnapshot, Long> {

        List<PromptTestSnapshot> findByVersionId(Long versionId);

        void deleteByCode(String code);

        @Query("SELECT s FROM PromptTestSnapshot s WHERE " +
                        "s.versionId = :versionId AND " +
                        "(:minSatisfaction IS NULL OR s.satisfaction >= :minSatisfaction) " +
                        "ORDER BY s.createdDatetime DESC")
        Page<PromptTestSnapshot> findByVersionIdAndMinSatisfaction(
                        @Param("versionId") Long versionId,
                        @Param("minSatisfaction") Integer minSatisfaction,
                        Pageable pageable);

        Page<PromptTestSnapshot> findByCode(String code, Pageable pageable);

        @Query("SELECT AVG(s.satisfaction) FROM PromptTestSnapshot s WHERE s.versionId = :versionId AND s.satisfaction IS NOT NULL")
        BigDecimal calculateAvgSatisfactionByVersionId(@Param("versionId") Long versionId);

        @Query("SELECT COUNT(s) FROM PromptTestSnapshot s WHERE s.versionId = :versionId")
        Integer countByVersionId(@Param("versionId") Long versionId);

        @Query("SELECT MAX(s.createdDatetime) FROM PromptTestSnapshot s WHERE s.versionId = :versionId")
        LocalDateTime findLastTestedAtByVersionId(@Param("versionId") Long versionId);

        @Query("SELECT COUNT(s) FROM PromptTestSnapshot s " +
                        "JOIN PromptVersion v ON s.versionId = v.id " +
                        "WHERE v.promptCode = :promptCode")
        Integer countByPromptCode(@Param("promptCode") String promptCode);

        @Query("SELECT MAX(s.createdDatetime) FROM PromptTestSnapshot s " +
                        "JOIN PromptVersion v ON s.versionId = v.id " +
                        "WHERE v.promptCode = :promptCode")
        LocalDateTime findLastTestDateByPromptCode(@Param("promptCode") String promptCode);

        @Query("SELECT AVG(s.satisfaction) FROM PromptTestSnapshot s " +
                        "JOIN PromptVersion v ON s.versionId = v.id " +
                        "WHERE v.promptCode = :promptCode AND s.satisfaction IS NOT NULL")
        BigDecimal calculateAvgSatisfactionByPromptCode(@Param("promptCode") String promptCode);

        @Query("SELECT COUNT(s) FROM PromptTestSnapshot s WHERE s.llmConfigId = :llmConfigId")
        Integer countByLlmConfigId(@Param("llmConfigId") Long llmConfigId);

        @Query(value = "SELECT * FROM prompt_test_snapshots s " +
                        "WHERE s.code = :code " +
                        "AND (:versionId IS NULL OR s.version_id = :versionId) " +
                        "AND (:model IS NULL OR s.llm_config ->> 'model' = :model) " +
                        "ORDER BY s.created_datetime DESC", countQuery = "SELECT count(*) FROM prompt_test_snapshots s "
                                        +
                                        "WHERE s.code = :code " +
                                        "AND (:versionId IS NULL OR s.version_id = :versionId) " +
                                        "AND (:model IS NULL OR s.llm_config ->> 'model' = :model)", nativeQuery = true)
        Page<PromptTestSnapshot> findAllSnapshots(
                        @Param("code") String code,
                        @Param("versionId") Long versionId,
                        @Param("model") String model,
                        Pageable pageable);
}
