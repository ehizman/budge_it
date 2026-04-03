-- Speeds up findByStatusAndCreatedAtBefore (stale job cleanup runs every 10 mins)
CREATE INDEX IF NOT EXISTS idx_ai_jobs_status_created_at
    ON advisor.ai_jobs (status, created_at)
    WHERE status = 'PENDING';

-- Speeds up findByJobId on budget_templates (called after every AI completion)
CREATE INDEX IF NOT EXISTS idx_budget_templates_job_id
    ON advisor.budget_templates (job_id);

-- Speeds up findByUserIdOrderByCreatedAtAsc (template selection screen)
CREATE INDEX IF NOT EXISTS idx_budget_templates_user_id
    ON advisor.budget_templates (user_id, created_at);

DROP INDEX IF EXISTS idx_budget_templates_user;