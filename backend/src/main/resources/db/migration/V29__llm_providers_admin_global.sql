-- V29: LLM providers are global ADMIN-managed resources.
-- OWNER users only select enabled providers; provider_code is globally unique again.

SET @drop_llm_providers_owner_fk = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE llm_providers DROP FOREIGN KEY fk_llm_providers_owner',
              'SELECT 1')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND constraint_name = 'fk_llm_providers_owner'
);
PREPARE drop_llm_providers_owner_fk_stmt FROM @drop_llm_providers_owner_fk;
EXECUTE drop_llm_providers_owner_fk_stmt;
DEALLOCATE PREPARE drop_llm_providers_owner_fk_stmt;

SET @drop_llm_providers_owner_code = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE llm_providers DROP INDEX uk_llm_providers_owner_code',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND index_name = 'uk_llm_providers_owner_code'
);
PREPARE drop_llm_providers_owner_code_stmt FROM @drop_llm_providers_owner_code;
EXECUTE drop_llm_providers_owner_code_stmt;
DEALLOCATE PREPARE drop_llm_providers_owner_code_stmt;

SET @drop_llm_providers_owner_idx = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE llm_providers DROP INDEX idx_llm_providers_owner',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND index_name = 'idx_llm_providers_owner'
);
PREPARE drop_llm_providers_owner_idx_stmt FROM @drop_llm_providers_owner_idx;
EXECUTE drop_llm_providers_owner_idx_stmt;
DEALLOCATE PREPARE drop_llm_providers_owner_idx_stmt;

SET @add_llm_providers_code_unique = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE llm_providers ADD UNIQUE KEY uk_llm_providers_code (provider_code)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND index_name = 'uk_llm_providers_code'
);
PREPARE add_llm_providers_code_unique_stmt FROM @add_llm_providers_code_unique;
EXECUTE add_llm_providers_code_unique_stmt;
DEALLOCATE PREPARE add_llm_providers_code_unique_stmt;

SET @drop_llm_providers_owner_column = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE llm_providers DROP COLUMN owner_id',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND column_name = 'owner_id'
);
PREPARE drop_llm_providers_owner_column_stmt FROM @drop_llm_providers_owner_column;
EXECUTE drop_llm_providers_owner_column_stmt;
DEALLOCATE PREPARE drop_llm_providers_owner_column_stmt;
