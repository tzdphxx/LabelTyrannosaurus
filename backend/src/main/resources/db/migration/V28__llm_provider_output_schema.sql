-- V28: Add output_schema_json to llm_providers for Admin-managed JSON Schema output structure.
-- The output schema is now managed by Admin at the Provider level, not by Owner in AI review configs.

ALTER TABLE llm_providers ADD COLUMN output_schema_json JSON NULL AFTER structured_output_mode;
