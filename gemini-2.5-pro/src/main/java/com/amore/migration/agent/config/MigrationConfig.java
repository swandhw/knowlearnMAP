package com.amore.migration.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class MigrationConfig {
  private Paths paths = new Paths();
  private Gemini gemini = new Gemini();
  private Build build = new Build();

  @Data
  public static class Paths {
    private String legacySourceDir;
    private String targetProjectRoot;
    private String targetPackagePath;
  }

  @Data
  public static class Gemini {
    private String modelName;
    private String apiKeyEnvVar;
  }

  @Data
  public static class Build {
    private String command;
    private String workDir;
  }
}
