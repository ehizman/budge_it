package com.relab.budgetpro.advisor.repository;

import com.relab.budgetpro.advisor.domain.BudgetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, UUID> {
    List<BudgetTemplate> findByUserId(UUID userId);
    List<BudgetTemplate> findByJobId(UUID jobId);
}
