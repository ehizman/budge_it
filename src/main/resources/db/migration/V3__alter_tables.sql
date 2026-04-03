DROP TABLE public.audit_log CASCADE;
CREATE SCHEMA security;

CREATE TABLE security.sse_tickets
(
    id         UUID         NOT NULL,
    job_id     UUID         NOT NULL,
    token      TEXT NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status     TEXT     NOT NULL,
    CONSTRAINT pk_sse_tickets PRIMARY KEY (id)
);

ALTER TABLE profile.financial_profiles
    ADD age_range TEXT;

ALTER TABLE profile.financial_profiles
    ADD debt JSONB;

ALTER TABLE profile.financial_profiles
    ADD employment_status TEXT;

ALTER TABLE profile.financial_profiles
    ADD expenses JSONB;

ALTER TABLE profile.financial_profiles
    ADD goals JSONB;

ALTER TABLE profile.financial_profiles
    ADD investment JSONB;

ALTER TABLE profile.financial_profiles
    ADD monthly_net_income DECIMAL(15, 2);

ALTER TABLE profile.financial_profiles
    ADD monthly_savings_target DECIMAL(15, 2);

ALTER TABLE profile.financial_profiles
DROP
COLUMN data;

ALTER TABLE profile.financial_profiles
    ALTER COLUMN created_at DROP NOT NULL;

ALTER TABLE profile.financial_profiles
    ALTER COLUMN updated_at DROP NOT NULL;

ALTER TABLE advisor.budget_templates
ALTER
COLUMN name TYPE TEXT USING (name::TEXT);

ALTER TABLE advisor.budget_templates
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE identity.refresh_tokens
DROP
CONSTRAINT refresh_tokens_user_id_fkey;

ALTER TABLE identity.users
ALTER
COLUMN account_number TYPE TEXT USING (account_number::TEXT);

ALTER TABLE identity.refresh_tokens
    ALTER COLUMN created_at DROP NOT NULL;

ALTER TABLE identity.users
    ALTER COLUMN created_at DROP NOT NULL;

ALTER TABLE identity.refresh_tokens
ALTER
COLUMN device_fingerprint TYPE TEXT USING (device_fingerprint::TEXT);

ALTER TABLE identity.users
ALTER
COLUMN email TYPE TEXT USING (email::TEXT);

ALTER TABLE identity.users
ALTER
COLUMN first_name TYPE TEXT USING (first_name::TEXT);

ALTER TABLE identity.users
ALTER
COLUMN last_name TYPE TEXT USING (last_name::TEXT);

ALTER TABLE identity.users
ALTER
COLUMN password_hash TYPE TEXT USING (password_hash::TEXT);

ALTER TABLE identity.users
ALTER
COLUMN status TYPE VARCHAR(255) USING (status::VARCHAR(255));

ALTER TABLE identity.refresh_tokens
ALTER
COLUMN token_hash TYPE VARCHAR(255) USING (token_hash::VARCHAR(255));