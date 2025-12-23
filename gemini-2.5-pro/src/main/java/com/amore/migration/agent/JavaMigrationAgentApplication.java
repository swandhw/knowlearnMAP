package com.amore.migration.agent;

import com.amore.migration.agent.processor.SourceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Paths;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@RequiredArgsConstructor
public class JavaMigrationAgentApplication implements CommandLineRunner {

  private final SourceProcessor sourceProcessor;

  public static void main(String[] args) {
    SpringApplication.run(JavaMigrationAgentApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("================================================================");
    log.info("🚀 Java Migration Agent Started");
    log.info("================================================================");

    if (args.length > 0) {
      String filePath = args[0];
      log.info("Mode: Single File Migration");
      sourceProcessor.prepareTargetDirectory(false); // Incremental: Keep others
      sourceProcessor.processSingleFile(Paths.get(filePath));
    } else {
      log.info("Mode: Full Scan Migration");
      sourceProcessor.processAllFiles(); // Inside this, it will do Full Clean
    }

    log.info("================================================================");
    log.info("🎉 Migration Process Completed");
    log.info("================================================================");
  }
}
