package com.amore.migration.agent.processor;

import com.amore.migration.agent.config.MigrationConfig;
import com.amore.migration.agent.dto.GeminiResponse;
import com.amore.migration.agent.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceProcessor {

  private final MigrationConfig config;
  private final GeminiService geminiService;

  public void prepareTargetDirectory(boolean cleanAll) {
    Path targetRoot = Paths.get(config.getPaths().getTargetProjectRoot());

    log.info("Preparing target directory: {} (Full Clean: {})", targetRoot, cleanAll);
    try {
      if (Files.exists(targetRoot)) {
        if (cleanAll) {
          log.info(" -> Cleaning existing target directory...");
          try (Stream<Path> stream = Files.list(targetRoot)) {
            stream.forEach(path -> {
              try {
                FileSystemUtils.deleteRecursively(path);
              } catch (IOException e) {
                log.error("Failed to delete: " + path, e);
              }
            });
          }
        } else {
          log.info(" -> Incremental mode: Preserving existing files.");
        }
      } else {
        Files.createDirectories(targetRoot);
        log.info(" -> Created new target directory.");
      }

      // Always ensure skeleton exists (safe to overwrite pom.xml)
      createMavenSkeleton(targetRoot);

    } catch (IOException e) {
      log.error("Failed to prepare target directory", e);
    }
  }

  private void createMavenSkeleton(Path targetRoot) throws IOException {
    Path pomPath = targetRoot.resolve("pom.xml");
    String pomContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.2.0</version>
                <relativePath/>
            </parent>
            <groupId>com.amore.migration</groupId>
            <artifactId>generated-project</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <name>generated-project</name>
            <properties>
                <java.version>17</java.version>
            </properties>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                </dependency>
                <dependency>
                    <groupId>org.mybatis.spring.boot</groupId>
                    <artifactId>mybatis-spring-boot-starter</artifactId>
                    <version>3.0.3</version>
                </dependency>
                <dependency>
                    <groupId>org.springdoc</groupId>
                    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                    <version>2.3.0</version>
                </dependency>
                <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <optional>true</optional>
                </dependency>
            </dependencies>
        </project>
        """;
    Files.writeString(pomPath, pomContent, StandardCharsets.UTF_8);
    log.info(" -> Created pom.xml skeleton.");

    Files.createDirectories(targetRoot.resolve("src/main/java"));
    Files.createDirectories(targetRoot.resolve("src/main/resources/mapper"));
  }

  public void processAllFiles() {
    prepareTargetDirectory(true); // Full Clean for all files
    Path sourceRoot = Paths.get(config.getPaths().getLegacySourceDir());
    try (Stream<Path> walk = Files.walk(sourceRoot)) {
      List<Path> files = walk
          .filter(p -> !Files.isDirectory(p))
          .filter(p -> p.toString().toLowerCase().endsWith(".sql") || p.toString().toLowerCase().endsWith(".plsql"))
          .collect(Collectors.toList());

      log.info("Found {} SQL files to process.", files.size());

      for (Path file : files) {
        processSingleFile(file);
      }
    } catch (IOException e) {
      log.error("Error scanning source directory", e);
    }
  }

  public void processSingleFile(Path filePath) {
    log.info("\nProcessing: {}", filePath);
    try {
      Path sourceRoot = Paths.get(config.getPaths().getLegacySourceDir());
      Path relativePath = sourceRoot.relativize(filePath);

      Path parent = relativePath.getParent();
      String subPackage = "";
      if (parent != null) {
        subPackage = Stream.of(parent.toString().split(Pattern.quote(FileSystems.getDefault().getSeparator())))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("."));
      }

      String fullPackage = subPackage.isEmpty() ? "com.amore.migration" : "com.amore.migration." + subPackage;
      String procedureName = getFileNameWithoutExtension(filePath).toUpperCase();
      String className = convertToPascalCase(procedureName);

      log.info(" -> Target Package: {}", fullPackage);
      log.info(" -> Procedure: {}, ClassName: {}", procedureName, className);

      String sourceContent = Files.readString(filePath, StandardCharsets.UTF_8);
      String prompt = constructPrompt(fullPackage, subPackage, procedureName, className, sourceContent);

      GeminiResponse response = geminiService.generateMigrationCode(prompt);
      saveGeneratedFiles(response);

    } catch (Exception e) {
      log.error("Error processing file: " + filePath, e);
    }
  }

  private void saveGeneratedFiles(GeminiResponse response) throws IOException {
    Path targetRoot = Paths.get(config.getPaths().getTargetProjectRoot());
    for (GeminiResponse.GeneratedFile genFile : response.getFiles()) {
      Path savePath = targetRoot.resolve(genFile.getPath());
      Files.createDirectories(savePath.getParent());
      Files.writeString(savePath, genFile.getContent(), StandardCharsets.UTF_8);
      log.info(" -> Saved: {}", savePath);
    }
  }

  private String getFileNameWithoutExtension(Path path) {
    String name = path.getFileName().toString();
    int lastIdx = name.lastIndexOf('.');
    return (lastIdx == -1) ? name : name.substring(0, lastIdx);
  }

  private String convertToPascalCase(String name) {
    return Arrays.stream(name.split("_"))
        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
        .collect(Collectors.joining());
  }

  private String constructPrompt(String fullPackage, String subPackage, String procedureName, String className,
      String sourceContent) {
    String subPackagePath = subPackage.replace(".", "/");
    return String.format("""
        You are an expert Java migration agent.
        Convert the following Oracle PL/SQL procedure into a Spring Boot REST API (Controller, Service, DTO, Mapper).

        [Context]
        - Target Build Tool: Maven
        - Framework: Spring Boot 3.2, MyBatis 3.0
        - Open API: SpringDoc OpenAPI (SwaggerUI)
        - Root Package: com.amore.migration
        - Current Target Package: %s
        - Procedure Name: %s
        - Java Class Name Base: %s
        - Output Format: JSON containing file paths and codes.

        [Legacy Procedure Source]
        ```sql
        %s
        ```

        [Parsing Rules - CRITICAL]
        1. **Block Comments**: Ignore content inside `/* ... */`.
        2. **Keywords**: Handle `IS` or `AS` appearing on new lines.
        3. **Parameters**: Correctly parse parameters with commas inside parentheses (e.g., `NUMBER(10,2)`).
        4. **Input/Output**: Identify IN parameters for Request DTO and OUT parameters for Response DTO.

        [Instruction]
        1. **Architecture**:
           - **Controller**: `src/main/java/com/amore/migration/%s/controller/%sController.java`
           - **Service**: `src/main/java/com/amore/migration/%s/service/%sService.java`
           - **DTO**: `src/main/java/com/amore/migration/%s/dto/%sRequest.java` and `%sResponse.java`
           - **Mapper Interface**: `src/main/java/com/amore/migration/%s/mapper/%sMapper.java`
           - **Mapper XML**: `src/main/resources/mapper/%s/%sMapper.xml`

        2. **Input/Output Mapping (CRITICAL)**:
           - **Controller Method Signature**: Use **Individual Parameters** with @RequestParam.
           - **Controller Implementation Pattern** (MANDATORY):
             ```java
             // 1. Create Request DTO
             {{ClassName}}Request request = new {{ClassName}}Request();
             // 2. Set each parameter via setter
             request.setPlant(plant);
             ...
             // 3. Call service with Request DTO
             {{ClassName}}Response response = service.execute(request);
             return ResponseEntity.ok(response);
             ```
           - **Service Method Signature**: Accept {{ClassName}}Request DTO.
           - **Response**: Create a dedicated DTO class ({{ClassName}}Response).

        3. **Swagger Integration**:
           - Controller: @Tag(name = "%s", description = "Migration API")
           - Endpoint: @Operation(summary = "Execute %s", description = "Calls stored procedure %s")

        4. **Return JSON Structure**:
        {
          "files": [
            {
              "path": "src/main/java/com/amore/migration/%s/controller/%sController.java",
              "content": "package %s.controller; ..."
            },
            ...
          ]
        }
        """, fullPackage, procedureName, className, sourceContent,
        subPackagePath, className, subPackagePath, className, subPackagePath, className, className,
        subPackagePath, className, subPackagePath, className,
        procedureName, procedureName, procedureName,
        subPackagePath, className, fullPackage);
  }
}
