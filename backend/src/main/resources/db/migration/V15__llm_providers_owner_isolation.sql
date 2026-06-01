-- V15: Migrate LLM Provider ownership from ADMIN to OWNER with per-owner isolation

ALTER TABLE llm_providers ADD COLUMN owner_id BIGINT NULL AFTER user_rate_limit_per_minute;

UPDATE llm_providers SET owner_id = created_by WHERE owner_id IS NULL;

CREATE TEMPORARY TABLE flyway_v15_llm_provider_owner_precheck AS
SELECT COUNT(*) AS ambiguous_owner_count
FROM llm_providers
WHERE owner_id IS NULL
  AND created_by IS NULL;

ALTER TABLE flyway_v15_llm_provider_owner_precheck
    ADD CONSTRAINT chk_llm_provider_owner_backfill_source
        CHECK (ambiguous_owner_count = 0);

DROP TEMPORARY TABLE flyway_v15_llm_provider_owner_precheck;

ALTER TABLE llm_providers MODIFY COLUMN owner_id BIGINT NOT NULL;

ALTER TABLE llm_providers ADD CONSTRAINT fk_llm_providers_owner FOREIGN KEY (owner_id) REFERENCES users(id);

ALTER TABLE llm_providers DROP INDEX uk_llm_providers_code;

ALTER TABLE llm_providers ADD UNIQUE KEY uk_llm_providers_owner_code (owner_id, provider_code);

ALTER TABLE llm_providers ADD INDEX idx_llm_providers_owner (owner_id);
