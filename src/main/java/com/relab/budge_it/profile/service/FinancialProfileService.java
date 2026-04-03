package com.relab.budge_it.profile.service;

import com.relab.budge_it.profile.domain.FinancialProfile;
import com.relab.budge_it.profile.domain.FinancialProfile.EmploymentStatus;
import com.relab.budge_it.profile.dto.ProfileDtos.ProfileResponse;
import com.relab.budge_it.profile.dto.ProfileDtos.UpsertProfileRequest;
import com.relab.budge_it.profile.event.ProfileEvents.FinancialProfileUpdatedEvent;
import com.relab.budge_it.profile.repository.FinancialProfileRepository;
import com.relab.budge_it.shared.response.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialProfileService {

    private final FinancialProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Upsert (create or update) ───────────────────────────────────────────

    /**
     * Creates the profile if it doesn't exist, updates it if it does.
     * Increments version on every update so AdvisorOrchestrationService
     * can snapshot which version of the profile Claude actually saw.
     */
    @Transactional
    public ProfileResponse upsert(UUID userId, UpsertProfileRequest request) {
        FinancialProfile profile = profileRepository.findByUserId(userId)
                .orElse(FinancialProfile.builder().userId(userId).build());

        boolean isUpdate = profile.getId() != null;

        // Map request fields onto the entity
        profile.setAgeRange(request.ageRange());
        profile.setMonthlyNetIncome(request.monthlyNetIncome());
        profile.setExpenses(request.expenses());
        profile.setDebt(request.debt());
        profile.setInvestment(request.investment());
        profile.setGoals(request.goals());
        profile.setMonthlySavingsTarget(request.monthlySavingsTarget());
        profile.setUpdatedAt(Instant.now());

        // Parse employment status safely
        if (request.employmentStatus() != null) {
            try {
                profile.setEmploymentStatus(
                        EmploymentStatus.valueOf(request.employmentStatus()));
            } catch (IllegalArgumentException e) {
                throw BusinessException.unprocessable("INVALID_EMPLOYMENT_STATUS",
                        "Valid values: EMPLOYED_FULL_TIME, SELF_EMPLOYED, FREELANCE, STUDENT, UNEMPLOYED");
            }
        }

        // Increment version on update — new profile starts at 1 (set by @Builder.Default)
        if (isUpdate) {
            profile.setVersion(profile.getVersion() + 1);
        }

        profile = profileRepository.save(profile);

        // Notify the rest of the system the profile changed
        eventPublisher.publishEvent(
                new FinancialProfileUpdatedEvent(userId, profile.getVersion()));

        log.info("Financial profile {} for user {} (version {})",
                isUpdate ? "updated" : "created", userId, profile.getVersion());

        return toResponse(profile);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> BusinessException.notFound("Financial profile"));
    }

    /**
     * Used by AdvisorOrchestrationService — returns the entity directly
     * so it can access version, monthlyNetIncome etc. without a DTO conversion.
     * Returns null if no profile exists (caller handles edge case AI-06).
     */
    @Transactional(readOnly = true)
    public FinancialProfile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.unprocessable("PROFILE_REQUIRED",
                        "Complete your financial profile before generating a budget."));
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private ProfileResponse toResponse(FinancialProfile p) {
        return new ProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getAgeRange(),
                p.getEmploymentStatus() != null ? p.getEmploymentStatus().name() : null,
                p.getMonthlyNetIncome(),
                p.getExpenses(),
                p.getDebt(),
                p.getInvestment(),
                p.getGoals(),
                p.getMonthlySavingsTarget(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}