package com.relab.budge_it.advisor.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relab.budge_it.advisor.domain.AIJob;
import com.relab.budge_it.advisor.domain.BudgetTemplate;
import com.relab.budge_it.advisor.dto.AdvisorDtos.*;
import com.relab.budge_it.advisor.event.AdvisorEvents.*;
import com.relab.budge_it.advisor.repository.AIJobRepository;
import com.relab.budge_it.advisor.repository.BudgetTemplateRepository;
import com.relab.budge_it.advisor.service.AdvisorOrchestrationService;
import com.relab.budge_it.shared.response.ApiResponse;
import com.relab.budge_it.shared.response.BusinessException;
import com.relab.budge_it.shared.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Advisor", description = "AI-driven budget generation, job tracking, and template management")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final AdvisorOrchestrationService orchestrationService;
    private final AIJobRepository jobRepository;
    private final BudgetTemplateRepository templateRepository;
    private final AuthenticatedUserProvider userProvider;
    private final ObjectMapper objectMapper;

    // Holds active SSE connections keyed by jobId
    // When a job completes, we push to the matching emitter
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ─── POST /advisor/generate ───────────────────────────────────────────────

    /**
     * Triggers AI budget generation.
     * Returns 202 Accepted immediately — generation happens asynchronously.
     * Client should poll GET /advisor/jobs/{jobId} or subscribe to the SSE stream.
     */
    @Operation(summary = "Trigger AI budget generation",
               description = "Starts an async AI generation job. Returns 202 immediately. Use the SSE stream or poll the job status endpoint to track completion.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Generation started"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Financial profile not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateResponse>> generate() {
        UUID userId = userProvider.getCurrentUserId();
        Map<String, Object> pair   = orchestrationService.initiateGeneration(userId);
        AIJob job = (AIJob) pair.get("job");
        String sseTicket = (String) pair.get("sseTicket");

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        new GenerateResponse(
                                job.getId(),
                                job.getStatus().name(),
                                sseTicket,
                                "Budget generation started. Poll /advisor/jobs/" + job.getId() + " for status."
                        ),
                        "Generation started"
                ));
    }

    // ─── GET /advisor/jobs/{jobId} ────────────────────────────────────────────

    /**
     * Poll this endpoint to check if Claude has finished.
     * Returns PENDING, COMPLETED, or FAILED with error details.
     */
    @Operation(summary = "Poll job status", description = "Returns the current status of a generation job (PENDING, COMPLETED, or FAILED).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found or does not belong to the user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<JobStatusResponse>> getJobStatus(
            @Parameter(description = "ID of the generation job") @PathVariable UUID jobId) {

        UUID userId = userProvider.getCurrentUserId();

        AIJob job = jobRepository.findById(jobId)
                .filter(j -> j.getUserId().equals(userId))
                .orElseThrow(() -> BusinessException.notFound("Job"));

        return ResponseEntity.ok(ApiResponse.success(toJobStatusResponse(job)));
    }

    // ─── GET /advisor/templates ───────────────────────────────────────────────

    /**
     * Returns all templates generated for the authenticated user.
     * Each template includes the full pocket breakdown from Claude.
     */
    @Operation(summary = "List all generated budget templates for the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Templates returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> listTemplates() {
        UUID userId = userProvider.getCurrentUserId();

        List<TemplateResponse> templates = templateRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::toTemplateResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    // ─── GET /advisor/templates/{id} ──────────────────────────────────────────

    /**
     * Returns a single template with full pocket breakdown.
     */
    @Operation(summary = "Get a single budget template by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found or does not belong to the user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
            @Parameter(description = "ID of the budget template") @PathVariable UUID id) {

        UUID userId = userProvider.getCurrentUserId();

        BudgetTemplate template = templateRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("Template"));

        return ResponseEntity.ok(ApiResponse.success(toTemplateResponse(template)));
    }

    // ─── PATCH /advisor/templates/{id}/save ──────────────────────────────────

    /**
     * Toggles the saved flag on a template.
     * Saved templates appear in the user's bookmarks for later reference.
     */
    @Operation(summary = "Toggle the saved flag on a template",
               description = "Saved templates appear in the user's bookmarks. Calling this endpoint toggles the flag.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template saved or unsaved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found or does not belong to the user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/templates/{id}/save")
    public ResponseEntity<ApiResponse<Void>> saveTemplate(
            @Parameter(description = "ID of the budget template") @PathVariable UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        BudgetTemplate template = templateRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("Template"));

        template.setSaved(!Boolean.TRUE.equals(template.getSaved()));
        templateRepository.save(template);

        String message = Boolean.TRUE.equals(template.getSaved())
                ? "Template saved"
                : "Template unsaved";

        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ─── GET /advisor/jobs/{jobId}/stream (SSE) ───────────────────────────────

    /**
     * Server-Sent Events stream for real-time job completion.
     * The client subscribes here immediately after POST /generate.
     * When the job completes or fails, we push one event and close the connection.
     *
     * Usage from the frontend:
     *   const es = new EventSource('/api/v1/advisor/jobs/{jobId}/stream');
     *   es.onmessage = (e) => console.log(JSON.parse(e.data));
     */
    @Operation(summary = "Subscribe to real-time job completion via SSE",
               description = "Server-Sent Events stream. The client subscribes immediately after POST /generate. One event is pushed when the job completes or fails, then the connection closes. Timeout: 5 minutes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE stream opened"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found or does not belong to the user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(
            @Parameter(description = "ID of the generation job") @PathVariable UUID jobId) {
        UUID userId = userProvider.getCurrentUserId();

        // Verify ownership before registering the emitter
        jobRepository.findById(jobId)
                .filter(j -> j.getUserId().equals(userId))
                .orElseThrow(() -> BusinessException.notFound("Job"));

        // 5 minute timeout — if generation takes longer the connection drops gracefully
        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.put(jobId, emitter);

        // Clean up when the client disconnects
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));

        // If the job already finished before the client subscribed, push immediately
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != AIJob.JobStatus.PENDING) {
                pushToEmitter(emitter, jobId, job.getStatus().name());
            }
        });

        return emitter;
    }

    // ─── SSE event listeners ──────────────────────────────────────────────────

    /**
     * Listens for BudgetTemplatesGeneratedEvent and pushes to any waiting SSE client.
     */
    @EventListener
    public void onTemplatesGenerated(BudgetTemplatesGeneratedEvent event) {
        SseEmitter emitter = emitters.get(event.jobId());
        if (emitter != null) {
            pushToEmitter(emitter, event.jobId(), "COMPLETED");
        }
    }

    /**
     * Listens for AIJobFailedEvent and pushes to any waiting SSE client.
     */
    @EventListener
    public void onJobFailed(AIJobFailedEvent event) {
        SseEmitter emitter = emitters.get(event.jobId());
        if (emitter != null) {
            pushToEmitter(emitter, event.jobId(), "FAILED");
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void pushToEmitter(SseEmitter emitter, UUID jobId, String status) {
        try {
            emitter.send(SseEmitter.event()
                    .name("job-update")
                    .data(Map.of("jobId", jobId, "status", status)));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to push SSE event for job {}: {}", jobId, e.getMessage());
            emitter.completeWithError(e);
        } finally {
            emitters.remove(jobId);
        }
    }

    private JobStatusResponse toJobStatusResponse(AIJob job) {
        return new JobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }

    private TemplateResponse toTemplateResponse(BudgetTemplate t) {
        List<PocketDefinition> pockets = List.of();
        try {
            pockets = objectMapper.readValue(
                    t.getPocketConfig(),
                    new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse pocket config for template {}: {}", t.getId(), e.getMessage());
        }
        return new TemplateResponse(
                t.getId(), t.getJobId(), t.getName(), t.getDescription(),
                pockets, t.getApplied(), t.getSaved(), t.getCreatedAt()
        );
    }
}