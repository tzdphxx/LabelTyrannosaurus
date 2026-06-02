-- V27: Migrate LLM Providers from Owner-managed to Admin-managed model.
-- Admin creates and manages encrypted API Keys; Owners only select from enabled models.

SET FOREIGN_KEY_CHECKS = 0;

-- Make owner_id nullable (Admin-created providers no longer have an owner).
ALTER TABLE llm_providers MODIFY COLUMN owner_id BIGINT NULL;

-- Drop owner-specific foreign key, unique constraint, and index from V15.
ALTER TABLE llm_providers DROP FOREIGN KEY fk_llm_providers_owner;
ALTER TABLE llm_providers DROP INDEX uk_llm_providers_owner_code;
ALTER TABLE llm_providers DROP INDEX idx_llm_providers_owner;

-- Restore unique constraint on provider_code alone (as in V1 baseline; V15 dropped it).
ALTER TABLE llm_providers ADD UNIQUE KEY uk_llm_providers_code (provider_code);

-- Make llm_trigger_runs.provider_id nullable (historical runs reference deleted providers).
ALTER TABLE llm_trigger_runs MODIFY COLUMN provider_id BIGINT NULL;

-- Clear AI review config references from tasks.
UPDATE tasks SET ai_review_config_id = NULL;

-- Null out provider references in historical execution records.
UPDATE agent_runs SET provider_id = NULL;
UPDATE ai_review_results SET provider_id = NULL;
UPDATE llm_trigger_runs SET provider_id = NULL;

-- Delete old owner-based AI review configs and providers.
-- Owners must reconfigure AI review with new Admin models.
DELETE FROM ai_review_configs;
DELETE FROM llm_providers;

SET FOREIGN_KEY_CHECKS = 1;
