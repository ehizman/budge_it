package com.relab.budge_it.advisor.service;

import com.relab.budge_it.advisor.repository.BudgetTemplateRepository;
import com.relab.budge_it.shared.response.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvisorQueryServiceImpl implements AdvisorQueryService {

    private final BudgetTemplateRepository templateRepository;

    @Override
    @Transactional(readOnly = true)
    public String getTemplatePocketConfig(UUID templateId, UUID userId) {
        return templateRepository.findPocketConfigByIdAndUserId(templateId, userId)
                .orElseThrow(() -> BusinessException.notFound("Template"))
                .getPocketConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public String getTemplateName(UUID templateId, UUID userId) {
        return templateRepository.findTemplateNameByIdAndUserId(templateId, userId)
                .map(BudgetTemplateRepository.TemplateNameView::getName)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTemplateAlreadyApplied(UUID templateId) {
        return templateRepository.findById(templateId)
                .map(t -> Boolean.TRUE.equals(t.getApplied()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void markTemplateApplied(UUID templateId) {
        templateRepository.findById(templateId).ifPresent(t -> {
            t.setApplied(true);
            t.setAppliedAt(Instant.now());
            templateRepository.save(t);
        });
    }
}