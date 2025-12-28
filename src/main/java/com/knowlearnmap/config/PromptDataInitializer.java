package com.knowlearnmap.config;

import com.knowlearnmap.prompt.domain.Prompt;
import com.knowlearnmap.prompt.domain.PromptRepository;
import com.knowlearnmap.prompt.domain.PromptVersion;
import com.knowlearnmap.prompt.domain.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptDataInitializer implements CommandLineRunner {

  private final PromptRepository promptRepository;
  private final PromptVersionRepository versionRepository;

  private static final String PROMPT_CODE = "CHUNK_TO_ONTOLOGY";
  private static final String DEFAULT_PROMPT_CONTENT = """
      You are an expert Ontology Engineer.
      Your task is to analyze the provided document chunks and extract ontology elements (Objects, Relations, and Triples) in JSON format.

      ### Input Data
      The input is a list of document chunks (JSON array with id and text):
      {{CHUNK_LIST}}

      ### Output Format (JSON)
      Return a valid JSON object. Do not include markdown formatting (```json ... ```).
      The logic handles both a root object or a "chunks" array. Ideally returns a "chunks" array structure corresponding to input.

      Structure:
      {
        "chunks": [
          {
            "id": 123,
            "objects_to_add": [
              {
                "category": "Person|Organization|Project|...",
                "term_en": "English Term",
                "term_ko": "Korean Term",
                "description_ko": "Description in Korean"
              }
            ],
            "relations_to_add": [
              {
                "category": "Connection|Hierarchical|...",
                "relation_en": "English Relation Name",
                "relation_ko": "Korean Relation Name",
                "description_ko": "Description"
              }
            ],
            "knowlearns_to_add": [
              {
                "subject_category": "...",
                "subject_term_en": "...",
                "subject_term_ko": "...",
                "relation_category": "...",
                "relation_en": "...",
                "relation_ko": "...",
                "object_category": "...",
                "object_term_en": "...",
                "object_term_ko": "...",
                "confidence_score": 0.9,
                "evidence_level": "High"
              }
            ]
          }
        ]
      }

      ### Instructions
      1. Extract meaningful entities references in the text.
      2. Infer relationships between them.
      3. Ensure Korean terms are populated if the text is Korean.
      4. Use confidence_score between 0.0 and 1.0.
      5. IMPORTANT: You MUST return the exact "id" from the input chunk in the corresponding output object so it can be mapped back.
      """;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    initializePrompt();
  }

  private void initializePrompt() {
    log.info("Checking for Prompt Code: {}", PROMPT_CODE);

    // 1. Check if Prompt exists
    Optional<Prompt> promptOpt = promptRepository.findByCode(PROMPT_CODE);
    Prompt prompt;

    if (promptOpt.isPresent()) {
      prompt = promptOpt.get();
      log.info("Prompt found: {}", prompt.getCode());
    } else {
      log.info("Prompt not found. Creating new Prompt: {}", PROMPT_CODE);
      prompt = Prompt.builder()
          .code(PROMPT_CODE)
          .name("Chunk To Ontology Extraction")
          .description("Extracts ontology objects and relations from document chunks")
          .isActive(true)
          .createdId("system")
          .updatedId("system")
          .createdDatetime(LocalDateTime.now())
          .updatedDatetime(LocalDateTime.now())
          .build();
      prompt = promptRepository.save(prompt);
    }

    // 2. Check if Active Version exists
    Optional<PromptVersion> activeVersionOpt = versionRepository.findByPromptCodeAndIsActive(PROMPT_CODE, true);

    if (activeVersionOpt.isPresent()) {
      log.info("Active version found for prompt: {}", PROMPT_CODE);
    } else {
      log.warn("No active version found for prompt: {}. Creating default version.", PROMPT_CODE);

      int versionNumber = 1;
      // Get max version
      Integer maxVer = versionRepository.findMaxVersionByPromptCode(PROMPT_CODE);
      if (maxVer != null) {
        versionNumber = maxVer + 1;
      }

      PromptVersion newVersion = PromptVersion.builder()
          .promptCode(PROMPT_CODE)
          .version(versionNumber)
          .content(DEFAULT_PROMPT_CONTENT)
          // Simple schema for variable
          .variableSchema(
              "[{\"key\":\"CHUNK_LIST\",\"type\":\"string\",\"description\":\"List of document chunks\"}]")
          .status("published")
          .isActive(true)
          .notes("Auto-generated by System Initializer")
          .createdId("system")
          .updatedId("system")
          .createdDatetime(LocalDateTime.now())
          .updatedDatetime(LocalDateTime.now())
          .build();

      newVersion = versionRepository.save(newVersion);

      // Update Prompt to point to this version
      prompt.setPublishVersionId(newVersion.getId());
      promptRepository.save(prompt);

      log.info("Created and activated version {} for prompt {}", versionNumber, PROMPT_CODE);
    }
  }
}
