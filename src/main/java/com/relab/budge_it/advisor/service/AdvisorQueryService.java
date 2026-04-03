package com.relab.budge_it.advisor.service;

import java.util.UUID;

/**
 * Public facade exposing advisor data to other modules.
 * Other modules import this interface — never the repository directly.
 */
public interface AdvisorQueryService {

    /**
     * Returns the raw pocket config JSON for a template.
     * Throws BusinessException NOT_FOUND if template doesn't belong to the user.
     */
    String getTemplatePocketConfig(UUID templateId, UUID userId);

    /**
     * Returns the template name, or null if not found.
     */
    String getTemplateName(UUID templateId, UUID userId);

    /**
     * Returns true if the template has already been applied.
     * Used by BudgetService to enforce edge case BG-04.
     */
    boolean isTemplateAlreadyApplied(UUID templateId);

    /**
     * Marks a template as applied so it cannot be applied twice.
     * Called by BudgetService after successfully creating a budget.
     */
    void markTemplateApplied(UUID templateId);
}