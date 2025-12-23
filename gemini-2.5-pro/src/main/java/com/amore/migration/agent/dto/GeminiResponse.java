package com.amore.migration.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class GeminiResponse {
  private List<GeneratedFile> files;

  @Data
  public static class GeneratedFile {
    private String path;
    private String content;
  }
}
