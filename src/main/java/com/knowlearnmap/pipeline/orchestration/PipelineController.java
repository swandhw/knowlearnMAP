package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for pipeline orchestration and monitoring.
 * 
 * <p>
 * Endpoints:
 * </p>
 * <ul>
 * <li>POST /api/pipeline/start - Start pipeline execution</li>
 * <li>GET /api/pipeline/status/{documentId} - Get current status</li>
 * <li>GET /api/pipeline/progress/{documentId} - Get detailed progress</li>
 * <li>GET /api/pipeline/executions/{workspaceId} - Get all executions for
 * workspace</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineOrchestrator orchestrator;
    private final PipelineExecutionRepository executionRepository;

    /**
     * Start pipeline execution for a document.
     * 
     * @param request Execution request
     * @return Execution response
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startPipeline(@RequestBody PipelineStartRequest request) {
        log.info("Starting pipeline for workspace={}, document={}",
                request.getWorkspaceId(), request.getDocumentId());

        try {
            // Start async execution
            CompletableFuture<PipelineContext> future = orchestrator.executeAsync(
                    request.getWorkspaceId(),
                    request.getDocumentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pipeline started successfully");
            response.put("workspaceId", request.getWorkspaceId());
            response.put("documentId", request.getDocumentId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start pipeline", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start pipeline: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get current pipeline status for a document.
     * 
     * @param documentId Document ID
     * @return Status response
     */
    @GetMapping("/status/{documentId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long documentId) {
        Optional<PipelineExecutionEntity> execution = executionRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId);

        if (execution.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PipelineExecutionEntity exec = execution.get();
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", exec.getDocumentId());
        response.put("workspaceId", exec.getWorkspaceId());
        response.put("status", exec.getStatus());
        response.put("currentStage", exec.getCurrentStage());
        response.put("progress", exec.getProgress());
        response.put("startTime", exec.getStartTime());
        response.put("endTime", exec.getEndTime());
        response.put("errorMessage", exec.getErrorMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed progress information.
     * 
     * @param documentId Document ID
     * @return Progress response
     */
    @GetMapping("/progress/{documentId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable Long documentId) {
        Optional<PipelineExecutionEntity> execution = executionRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId);

        if (execution.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PipelineExecutionEntity exec = execution.get();
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", exec.getDocumentId());
        response.put("status", exec.getStatus());
        response.put("currentStage", exec.getCurrentStage());
        response.put("progress", exec.getProgress());
        response.put("startTime", exec.getStartTime());

        // Calculate elapsed time if running
        if (exec.getEndTime() == null && exec.getStartTime() != null) {
            response.put("running", true);
            response.put("elapsedSeconds",
                    java.time.Duration.between(exec.getStartTime(), java.time.LocalDateTime.now()).getSeconds());
        } else {
            response.put("running", false);
            if (exec.getEndTime() != null) {
                response.put("totalSeconds",
                        java.time.Duration.between(exec.getStartTime(), exec.getEndTime()).getSeconds());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get all executions for a workspace.
     * 
     * @param workspaceId Workspace ID
     * @return List of executions
     */
    @GetMapping("/executions/{workspaceId}")
    public ResponseEntity<List<PipelineExecutionEntity>> getExecutions(@PathVariable Long workspaceId) {
        List<PipelineExecutionEntity> executions = executionRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return ResponseEntity.ok(executions);
    }
}
