-- V20: Add structured output mode to LLM providers.
-- Controls whether the gateway sends OpenAI response_format (NONE/JSON_OBJECT/JSON_SCHEMA).
-- Column guard keeps this migration safe for databases whose baseline already contains the column.

SET @add_llm_providers_structured_output_mode = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE llm_providers ADD COLUMN structured_output_mode VARCHAR(20) NOT NULL DEFAULT ''NONE'' COMMENT ''NONE, JSON_OBJECT, JSON_SCHEMA''',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_providers'
      AND column_name = 'structured_output_mode'
);
PREPARE add_llm_providers_structured_output_mode_stmt FROM @add_llm_providers_structured_output_mode;
EXECUTE add_llm_providers_structured_output_mode_stmt;
DEALLOCATE PREPARE add_llm_providers_structured_output_mode_stmt;
