package com.relab.budge_it.advisor.repository;

import com.relab.budge_it.advisor.domain.BudgetTemplate;
import io.lettuce.core.ScanIterator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, UUID> {
    Optional<BudgetTemplate> findByIdAndUserId(UUID id, UUID userId);
    Optional<PocketConfigView> findPocketConfigByIdAndUserId(UUID userId, UUID jobId);
    Optional<TemplateNameView> findTemplateNameByIdAndUserId(UUID userId, UUID jobId);
    List<BudgetTemplate> findByUserIdOrderByCreatedAtAsc(UUID userId);

    public interface PocketConfigView {
        String getPocketConfig();
    }

    public interface TemplateNameView {
        String getName();
    }
}


