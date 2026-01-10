package com.knowlearnmap.chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDto {
    private Long workspaceId;
    private String message;
    private List<Long> documentIds;
}
