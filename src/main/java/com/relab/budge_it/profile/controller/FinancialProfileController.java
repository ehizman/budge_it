package com.relab.budge_it.profile.controller;


import com.relab.budge_it.profile.dto.ProfileDtos.ProfileResponse;
import com.relab.budge_it.profile.dto.ProfileDtos.UpsertProfileRequest;
import com.relab.budge_it.profile.repository.FinancialProfileRepository;
import com.relab.budge_it.profile.service.FinancialProfileService;
import com.relab.budge_it.shared.response.ApiResponse;
import com.relab.budge_it.shared.response.BusinessException;
import com.relab.budge_it.shared.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/financial-profile")
@RequiredArgsConstructor
public class FinancialProfileController {

    private final FinancialProfileService profileService;
    private final FinancialProfileRepository profileRepository;
    private final AuthenticatedUserProvider userProvider;

    // ─── GET /users/me/financial-profile ─────────────────────────────────────

    /**
     * Returns the authenticated user's financial profile.
     * Returns 404 if the user hasn't completed the survey yet.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(userId)));
    }

    // ─── PUT /users/me/financial-profile ──────────────────────────────────────

    /**
     * Creates the profile if it doesn't exist, updates it if it does.
     * Increments the profile version on every call.
     * The AI advisor uses the version to know which profile it generated against.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> upsertProfile(
            @Valid @RequestBody UpsertProfileRequest request) {

        UUID userId = userProvider.getCurrentUserId();
        ProfileResponse response = profileService.upsert(userId, request);

        boolean isNew = response.version() == 1;
        String message = isNew ? "Financial profile created" : "Financial profile updated";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    // ─── DELETE /users/me/financial-profile ───────────────────────────────────

    /**
     * Deletes the user's financial profile.
     * Note: this does not delete any generated templates or budgets —
     * those belong to the advisor and budget modules respectively.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfile() {
        UUID userId = userProvider.getCurrentUserId();

        profileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("Financial profile"));

        profileRepository.deleteByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Financial profile deleted"));
    }
}