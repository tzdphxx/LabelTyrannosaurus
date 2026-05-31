-- V15: Migrate LLM Provider ownership from ADMIN to OWNER with per-owner isolation

ALTER TABLE llm_providers ADD COLUMN owner_id BIGINT NULL AFTER user_rate_limit_per_minute;

UPDATE llm_providers SET owner_id = created_by WHERE owner_id IS NULL;

ALTER TABLE llm_providers MODIFY COLUMN owner_id BIGINT NOT NULL;

ALTER TABLE llm_providers ADD CONSTRAINT fk_llm_providers_owner FOREIGN KEY (owner_id) REFERENCES users(id);

ALTER TABLE llm_providers DROP INDEX uk_llm_providers_code;

ALTER TABLE llm_providers ADD UNIQUE KEY uk_llm_providers_owner_code (owner_id, provider_code);

ALTER TABLE llm_providers ADD INDEX idx_llm_providers_owner (owner_id);
