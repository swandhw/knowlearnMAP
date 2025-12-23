package com.knowlearnmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * KnowlearnMAP Backend Application
 * 
 * 확장 가능한 엔터프라이즈급 백엔드 시스템
 * - Workspace Management
 * - Document Management
 * - 향후: RAG, LLM, Ontology, ArangoDB 연동
 */
@SpringBootApplication(scanBasePackages = "com.knowlearnmap")
@EnableAsync
@EnableJpaRepositories(basePackages = "com.knowlearnmap")
@EntityScan(basePackages = "com.knowlearnmap")
public class KnowlearnMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowlearnMapApplication.class, args);
    }
}
