-- ============================================================
-- V1__init_schemas_and_tables.sql
-- ReLab Budget — initial database setup
-- Each module owns its own schema.
-- Cross-module references store UUID; no cross-schema FK constraints.
-- ============================================================

-- ── SCHEMAS ──────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS profile;
CREATE SCHEMA IF NOT EXISTS advisor;
CREATE SCHEMA IF NOT EXISTS budget;
CREATE SCHEMA IF NOT EXISTS pocket;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS recurring;
CREATE SCHEMA IF NOT EXISTS notification;

-- ── IDENTITY SCHEMA ──────────────────────────────────────────
CREATE TABLE identity.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      TEXT    NOT NULL,
    last_name       TEXT    NOT NULL,
    email           TEXT    UNIQUE NOT NULL,
    password_hash   TEXT    NOT NULL,
    phone           VARCHAR(20),
    account_number  VARCHAR(20)     UNIQUE NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE identity.refresh_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    token_hash          TEXT UNIQUE NOT NULL,
    device_fingerprint  TEXT,
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked             BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id  ON identity.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires  ON identity.refresh_tokens(expires_at);

-- ── PROFILE SCHEMA ───────────────────────────────────────────
CREATE TABLE profile.financial_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        UNIQUE NOT NULL,
    data                    JSONB       NOT NULL,
    version                 INTEGER     NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── ADVISOR SCHEMA ───────────────────────────────────────────
CREATE TABLE advisor.ai_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    profile_version INTEGER     NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    error_code      VARCHAR(100),
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_ai_jobs_user_status ON advisor.ai_jobs(user_id, status);

CREATE TABLE advisor.budget_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID        NOT NULL REFERENCES advisor.ai_jobs(id),
    user_id         UUID        NOT NULL,
    name            TEXT,
    description     TEXT,
    pocket_config   JSONB       NOT NULL,
    applied         BOOLEAN     NOT NULL DEFAULT FALSE,
    applied_at      TIMESTAMPTZ,
    saved           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_budget_templates_user ON advisor.budget_templates(user_id);

-- ── BUDGET SCHEMA ────────────────────────────────────────────
CREATE TABLE budget.budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        UNIQUE NOT NULL,
    template_id UUID,
    name        TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE budget.budget_configurations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    pocket_snapshot JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budget_configs_user ON budget.budget_configurations(user_id);

-- ── POCKET SCHEMA ────────────────────────────────────────────
CREATE TABLE pocket.pockets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id   UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    name        TEXT NOT NULL,
    type        TEXT  NOT NULL,
    target    NUMERIC(15,2) NOT NULL DEFAULT 0
        CHECK ( target >= 0 ),
    available     NUMERIC(15,2) NOT NULL DEFAULT 0
                    CHECK (available >= 0),
    icon        VARCHAR(50),
    color       VARCHAR(7),
    priority    INTEGER     NOT NULL DEFAULT 99,
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE,
    locked      BOOLEAN     NOT NULL DEFAULT FALSE,
    lock_until  TIMESTAMPTZ,
    CONSTRAINT chk_lock CHECK (locked = FALSE OR lock_until IS NOT NULL),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pockets_budget_id ON pocket.pockets(budget_id);
CREATE INDEX idx_pockets_user_id   ON pocket.pockets(user_id);

CREATE TABLE pocket.pocket_transfers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    from_pocket_id  UUID        NOT NULL REFERENCES pocket.pockets(id),
    to_pocket_id    UUID        NOT NULL REFERENCES pocket.pockets(id),
    amount          NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── WALLET SCHEMA ────────────────────────────────────────────
CREATE TABLE wallet.transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    type                VARCHAR(10) NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    amount              NUMERIC(15,2) NOT NULL,
    sender_name         TEXT,
    sender_account      VARCHAR(20),
    sender_bank         TEXT,
    bank_reference      TEXT UNIQUE,   -- deduplication key (edge case WL-01)
    payment_request_id  UUID,
    pocket_id           UUID,
    note                TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'SUCCESSFUL'
                            CHECK (status IN ('SUCCESSFUL', 'FAILED', 'REVERSED', 'PENDING_REVIEW')),
    settled_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user    ON wallet.transactions(user_id);
CREATE INDEX idx_transactions_pocket  ON wallet.transactions(pocket_id);
CREATE INDEX idx_transactions_ref     ON wallet.transactions(bank_reference);

CREATE TABLE wallet.payment_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    reference           TEXT UNIQUE NOT NULL,
    label               TEXT NOT NULL,
    amount              NUMERIC(15,2), -- NULL = any amount accepted
            CHECK ( amount > 0 ),
    target_pocket_id    UUID, -- NULL = default Spend pocket
    note                TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'SETTLED', 'EXPIRED', 'CANCELLED', 'PARTIAL')),
    expires_at          TIMESTAMPTZ,
    transaction_id      UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_requests_user      ON wallet.payment_requests(user_id);
CREATE INDEX idx_payment_requests_reference ON wallet.payment_requests(reference);

-- ── RECURRING SCHEMA ─────────────────────────────────────────
CREATE TABLE recurring.recurring_payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL,
    pocket_id               UUID        NOT NULL,
    label                   TEXT NOT NULL,
    amount                  NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    frequency               VARCHAR(20) NOT NULL
                                CHECK (frequency IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','ANNUALLY')),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','CANCELLED','FAILED')),
    start_date              DATE        NOT NULL,
    end_date                DATE,
    anchor_day              INTEGER,    -- original day-of-month (edge case RC-04)
    next_run_date           DATE        NOT NULL,
    last_run_date           DATE,
    failure_count           INTEGER     NOT NULL DEFAULT 0,
    consecutive_failures    INTEGER     NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recurring_next_run ON recurring.recurring_payments(next_run_date, status);
CREATE INDEX idx_recurring_pocket   ON recurring.recurring_payments(pocket_id);

CREATE TABLE recurring.recurring_execution_logs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recurring_payment_id    UUID        NOT NULL REFERENCES recurring.recurring_payments(id),
    run_date                DATE        NOT NULL,
    status                  VARCHAR(10) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    failure_reason          TEXT,
    amount_debited          NUMERIC(15,2),
    pocket_balance_before   NUMERIC(15,2),
    pocket_balance_after    NUMERIC(15,2),
    executed_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── NOTIFICATION SCHEMA ──────────────────────────────────────
CREATE TABLE notification.notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    title       TEXT NOT NULL,
    body        TEXT        NOT NULL,
    type        VARCHAR(50),
    reference_id UUID,
    read        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notification.notifications(user_id, read);

-- ── AUDIT LOG (append-only) ───────────────────────────────────
CREATE TABLE public.audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    action          TEXT NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    amount_before   NUMERIC(15,2),
    amount_after    NUMERIC(15,2),
    ip_address      VARCHAR(45),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user ON public.audit_log(user_id, created_at DESC);

create table event_publication
(
    completion_attempts    integer                     not null,
    completion_date        timestamp(6) with time zone,
    last_resubmission_date timestamp(6) with time zone,
    publication_date       timestamp(6) with time zone not null,
    id                     uuid                        not null primary key,
    event_type             TEXT                not null,
    listener_id            TEXT                not null,
    serialized_event       TEXT                not null,
    status                 TEXT
        constraint event_publication_status_check
            check ((status)::text = ANY
        ((ARRAY ['PUBLISHED'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'RESUBMITTED'::character varying])::text[]))
    );

alter table event_publication
    owner to relab;

