package com.knowlearnmap.chat.controller;

import com.knowlearnmap.chat.dto.ChatRequestDto;
import com.knowlearnmap.chat.dto.ChatResponseDto;
import com.knowlearnmap.chat.service.ChatService;
import com.knowlearnmap.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Chat endpoint with RAG and Ontology search
     * 
     * @param request Chat request containing message, workspaceId, and optional
     *                documentIds
     * @return Chat response with RAG and ontology search results
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatResponseDto>> sendMessage(@RequestBody ChatRequestDto request) {
        log.info("POST /api/chat/send - workspaceId: {}, message: {}",
                request.getWorkspaceId(), request.getMessage());

        ChatResponseDto response = chatService.chat(
                request.getMessage(),
                request.getWorkspaceId(),
                request.getDocumentIds());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
