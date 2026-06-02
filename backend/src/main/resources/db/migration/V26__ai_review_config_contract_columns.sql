SET @add_ai_review_configs_scoring_dimensions_json = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN scoring_dimensions_json JSON NULL AFTER prompt_template',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'scoring_dimensions_json'
);
PREPARE stmt FROM @add_ai_review_configs_scoring_dimensions_json;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @copy_ai_review_configs_scoring_dimensions_json = (
    SELECT IF(COUNT(*) = 1,
        'UPDATE ai_review_configs SET scoring_dimensions_json = dimension_config_json WHERE scoring_dimensions_json IS NULL AND dimension_config_json IS NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'dimension_config_json'
);
PREPARE stmt FROM @copy_ai_review_configs_scoring_dimensions_json;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_manual_review_threshold = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN manual_review_threshold DECIMAL(5,2) NULL AFTER pass_threshold',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'manual_review_threshold'
);
PREPARE stmt FROM @add_ai_review_configs_manual_review_threshold;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @copy_ai_review_configs_manual_review_threshold = (
    SELECT IF(COUNT(*) = 1,
        'UPDATE ai_review_configs SET manual_review_threshold = manual_threshold WHERE manual_review_threshold IS NULL AND manual_threshold IS NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'manual_threshold'
);
PREPARE stmt FROM @copy_ai_review_configs_manual_review_threshold;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_prompt_version = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN prompt_version VARCHAR(64) NOT NULL DEFAULT ''v1'' AFTER output_schema_json',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'prompt_version'
);
PREPARE stmt FROM @add_ai_review_configs_prompt_version;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_enabled_tools_json = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN enabled_tools_json JSON NULL AFTER agent_mode',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'enabled_tools_json'
);
PREPARE stmt FROM @add_ai_review_configs_enabled_tools_json;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @copy_ai_review_configs_enabled_tools_json = (
    SELECT IF(COUNT(*) = 1,
        'UPDATE ai_review_configs SET enabled_tools_json = enabled_tools WHERE enabled_tools_json IS NULL AND enabled_tools IS NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'enabled_tools'
);
PREPARE stmt FROM @copy_ai_review_configs_enabled_tools_json;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_multimodal_enabled = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN multimodal_enabled TINYINT(1) NOT NULL DEFAULT 1',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'multimodal_enabled'
);
PREPARE stmt FROM @add_ai_review_configs_multimodal_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_degradation_penalty = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN degradation_penalty DECIMAL(3,2) DEFAULT 0.20',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'degradation_penalty'
);
PREPARE stmt FROM @add_ai_review_configs_degradation_penalty;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_vision_detail = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN vision_detail VARCHAR(20) DEFAULT ''auto''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'vision_detail'
);
PREPARE stmt FROM @add_ai_review_configs_vision_detail;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_max_images_per_request = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN max_images_per_request INT DEFAULT 5',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'max_images_per_request'
);
PREPARE stmt FROM @add_ai_review_configs_max_images_per_request;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_ai_review_configs_allow_degraded = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD COLUMN allow_ai_direct_approve_when_degraded TINYINT(1) NOT NULL DEFAULT 0',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'allow_ai_direct_approve_when_degraded'
);
PREPARE stmt FROM @add_ai_review_configs_allow_degraded;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
