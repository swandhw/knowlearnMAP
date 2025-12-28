package com.knowlearnmap.prompt.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePromptRequest {
    private String name;
    private String description;
    private List<String> tags;
}
