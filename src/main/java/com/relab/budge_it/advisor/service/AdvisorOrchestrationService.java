package com.relab.budge_it.advisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relab.budge_it.advisor.domain.AIJob;
import com.relab.budge_it.advisor.domain.BudgetTemplate;
import com.relab.budge_it.advisor.dto.AdvisorDtos.PocketDefinition;
import com.relab.budge_it.advisor.event.AdvisorEvents;
import com.relab.budge_it.advisor.repository.AIJobRepository;
import com.relab.budge_it.advisor.repository.BudgetTemplateRepository;
import com.relab.budge_it.shared.security.JwtService;
import com.relab.budge_it.shared.security.SSETicket;
import com.relab.budge_it.shared.security.SSETicketRepository;
import com.relab.budge_it.shared.response.BusinessException;
import com.relab.budge_it.profile.service.FinancialProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorOrchestrationService {

    private final AIJobRepository jobRepository;
    private final BudgetTemplateRepository templateRepository;
    private final FinancialProfileService profileService;
    private final ApplicationEventPublisher eventPublisher;
    private final OpenAiChatModel chatModel;
//    private AnthropicChatModel anthropicChatModel;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final SSETicketRepository sseTicketRepository;

    private static final int MAX_RETRIES = 2;

    private static final String SYSTEM_PROMPT = """
            You are a professional personal finance advisor for Nigerian users.
            Your purpose is to suggest a path to building wealth and being financially free in 1-5 years
            Given a user's financial profile, generate exactly 3 distinct budget templates.
            
            Rules:
            1. Total suggested allocations must NOT exceed the user's monthly net income.
            2. Always include a "Spend" pocket as the first entry with isDefault: true.
            3. Do not suggest an amount for the "Spend" pocket — leave it blank for the user to fill in based on their lifestyle. This encourages user engagement and ownership of the budget.
            4. Each template must reflect a different financial philosophy:
               - Template 1: Balanced (giving to God (if the person is religious), moderate savings, debt, charity, investment and lifestyle)
               - Template 2: Aggressive Savings (maximise savings and investment)
               - Template 3: Debt-First (prioritise debt clearance)
            4. Respond ONLY with a valid JSON array. No explanation, no markdown.
            
            JSON format:
            [
              {
                "name": "Template Name",
                "description": "Brief description",
                "pockets": [
                  {
                    "name": "Pocket Name",
                    "type": "SPEND",
                    "suggestedAmount": 50000.00,
                    "percentage": 10,
                    "icon": "wallet",
                    "color": "#FF5733",
                    "priority": 1,
                    "isDefault": true
                  }
                ]
              }
            ]
            
            Valid pocket types: SPEND, SAVINGS, DEBT, INVESTMENT, HOUSING,
            TRANSPORT, FEEDING, UTILITIES, HEALTHCARE, EMERGENCY,
            TRAVEL, GIFTS, EDUCATION, CUSTOM
            """;

    // ─── Step 1: User triggers generation ────────────────────────────────────

    /**
     * Called by the controller. Creates an AIJob and publishes an event.
     * Returns immediately with 202 — the actual Claude call happens in Step 2.
     */
    @Transactional
    public Map<String, Object> initiateGeneration(UUID userId) {
        var profile = profileService.getProfileOrThrow(userId);

        // Edge case AI-06: no profile exists
        if (profile == null) {
            throw BusinessException.unprocessable("PROFILE_REQUIRED",
                    "Complete your financial profile before generating a budget.");
        }

        // Edge case AI-03: a PENDING job already exists — return it instead of creating a duplicate
        var existing = jobRepository.findByUserIdAndStatus(userId, AIJob.JobStatus.PENDING);
        if (existing.isPresent()) {
            log.info("Returning existing pending job {} for user {}", existing.get().getId(), userId);
            // find SSE Ticket by jobID
            var sseTicket = sseTicketRepository.findFirstByJobIdAndStatusIsActive(existing.get().getId())
                    .orElseThrow(() -> BusinessException.notFound("SSE Ticket"));
            return Map.of("job", existing.get(), "sseTicket", sseTicket);
        }

        AIJob job = AIJob.builder()
                .userId(userId)
                .profileVersion(profile.getVersion())
                .build();
        job = jobRepository.save(job);

        //generate SSE ticket that expires after 60 secs
        SSETicket sseTicket = jwtService.generateAndSaveSSETicket(job.getId());

        // Publish event — Step 2 listens to this and calls Claude asynchronously
        eventPublisher.publishEvent(
                new AdvisorEvents.FinancialProfileSubmittedEvent(
                        job.getId(), userId, profile.getVersion()));

        log.info("AI generation job {} created for user {}", job.getId(), userId);
        return Map.of("job", job, "sseTicket", sseTicket);
    }

    // ─── Step 2: Call AI Model asynchronously ──────────────────────────────────

    /**
     * Listens for FinancialProfileSubmittedEvent AFTER the transaction commits.
     * Runs on the aiAdvisorExecutor thread pool — never blocks the main thread.
     * Implements retry logic for edge cases AI-01 (bad JSON) and AI-02 (timeout).
     */
    @Async("aiAdvisorExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileSubmitted(AdvisorEvents.FinancialProfileSubmittedEvent event) {
        log.info("Processing AI job {} for user {}", event.jobId(), event.userId());

        AIJob job = jobRepository.findById(event.jobId()).orElseThrow();
        var profile = profileService.getProfileOrThrow(event.userId());

        String profileJson;
        try {
            profileJson = objectMapper.writeValueAsString(profile);
        } catch (Exception e) {
            markJobFailed(job, "PROFILE_SERIALIZATION_FAILED", e.getMessage());
            return;
        }

        int attempt = 0;
        String lastError = null;

        while (attempt <= MAX_RETRIES) {
            try {
                String userMessage = buildUserMessage(profileJson, attempt);
                log.info("Calling AI Model for job {} attempt {}", event.jobId(), attempt + 1);

                // Call Claude
                String raw = chatModel.call(SYSTEM_PROMPT + "\n\nUser: " + userMessage);


                // TODO - implement a redundancy system that listens for this exception and switches to another model for the next retry
                if (Objects.isNull(raw) || raw.isBlank()) {
                    // switch to another model for the next retry
                    // chatModel = anotherChatModel;
                    continue;
                }

                // Strip any accidental markdown fences
                String cleaned = raw.replaceAll("(?s)```json|```", "").trim();

                // Parse and validate the response
                List<TemplateData> templates = parseTemplates(cleaned);
                validateAllocations(templates, profile.getMonthlyNetIncome());

                // Persist templates and mark job complete
                List<UUID> ids = saveTemplates(job, templates);
                completeJob(job, ids);
                return;

            } catch (Exception e) {
                lastError = e.getMessage();
                attempt++;
                log.warn("AI job {} attempt {} failed: {}", event.jobId(), attempt, lastError);
            }
        }

        // All retries exhausted
        markJobFailed(job, "AI_GENERATION_FAILED", lastError);
    }

    // ─── Scheduled: stale job cleanup ────────────────────────────────────────

    /**
     * Edge case AI-02: jobs stuck in PENDING for more than 5 minutes are marked FAILED.
     * Runs every 10 minutes.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void cleanUpStaleJobs() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<AIJob> stale = jobRepository.findByStatusAndCreatedAtBefore(
                AIJob.JobStatus.PENDING, cutoff);

        for (AIJob job : stale) {
            log.warn("Marking stale job {} as FAILED", job.getId());
            markJobFailed(job, "JOB_TIMEOUT", "Job exceeded 5 minute processing limit.");
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String buildUserMessage(String profileJson, int attempt) {
        if (attempt == 0) {
            return "Generate 3 budget templates for this financial profile:\n" + profileJson;
        }
        // Edge case AI-01: corrective prompt on retry
        return "Your previous response was not valid JSON. " +
                "Return ONLY the JSON array, no explanation, no markdown:\n" + profileJson;
    }

    private List<TemplateData> parseTemplates(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<List<TemplateData>>() {});
    }

    /**
     * Edge case AI-04: if Claude suggests more than the user's income,
     * scale all amounts proportionally to fit within income.
     */
    private void validateAllocations(List<TemplateData> templates, BigDecimal monthlyIncome) {
        if (monthlyIncome == null) return;

        for (TemplateData template : templates) {
            BigDecimal total = template.pockets().stream()
                    .map(PocketDefinition::suggestedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.compareTo(monthlyIncome) > 0) {
                log.warn("AI suggested {} but income is {} — scaling down", total, monthlyIncome);
                BigDecimal scale = monthlyIncome.divide(total, 10, java.math.RoundingMode.HALF_UP);
                List<PocketDefinition> scaled = template.pockets().stream().map(p ->
                        p.withSuggestedAmount(
                                p.suggestedAmount().multiply(scale).setScale(2, RoundingMode.HALF_UP)
                        )
                ).toList();
                int index = templates.indexOf(template);
                templates.set(index, new TemplateData(template.name(), template.description(), scaled));
            }
        }
    }

    private List<UUID> saveTemplates(AIJob job, List<TemplateData> templates) throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (TemplateData t : templates) {
            BudgetTemplate entity = BudgetTemplate.builder()
                    .job(job)
                    .userId(job.getUserId())
                    .name(t.name())
                    .description(t.description())
                    .pocketConfig(objectMapper.writeValueAsString(t.pockets()))
                    .build();
            entity = templateRepository.save(entity);
            ids.add(entity.getId());
        }
        return ids;
    }

    private void completeJob(AIJob job, List<UUID> templateIds) {
        job.setStatus(AIJob.JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
        eventPublisher.publishEvent(new AdvisorEvents.BudgetTemplatesGeneratedEvent(
                job.getId(), job.getUserId(), templateIds));
        log.info("AI job {} completed with {} templates", job.getId(), templateIds.size());
    }

    private void markJobFailed(AIJob job, String code, String message) {
        job.setStatus(AIJob.JobStatus.FAILED);
        job.setErrorCode(code);
        job.setErrorMessage(message);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
        eventPublisher.publishEvent(new AdvisorEvents.AIJobFailedEvent(
                job.getId(), job.getUserId(), code));
        log.error("AI job {} failed with code {}: {}", job.getId(), code, message);
    }

    // ─── Internal record for parsing Claude's response ────────────────────────

    /**
     * Maps directly to Claude's JSON output structure.
     * Used only inside this service — not exposed in the API.
     */
    private record TemplateData(
            String name,
            String description,
            List<PocketDefinition> pockets
    ) {}
}