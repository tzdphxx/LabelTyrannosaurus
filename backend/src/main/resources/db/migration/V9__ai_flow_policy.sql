-- AI auto-review flow policy support.
-- Column guards keep this migration safe for fresh databases whose baseline
-- already contains part of the review threshold model.

SET @add_ai_review_configs_ai_flow_policy = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN ai_flow_policy VARCHAR(30) NOT NULL DEFAULT ''MANUAL_FIRST'' COMMENT ''MANUAL_FIRST, AI_PASS_ONLY, AI_REJECT_ONLY, AI_PASS_AND_REJECT, ALWAYS_MANUAL''',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'ai_flow_policy'
);
PREPARE add_ai_review_configs_ai_flow_policy_stmt FROM @add_ai_review_configs_ai_flow_policy;
EXECUTE add_ai_review_configs_ai_flow_policy_stmt;
DEALLOCATE PREPARE add_ai_review_configs_ai_flow_policy_stmt;

SET @add_ai_review_configs_allow_ai_direct_approve = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN allow_ai_direct_approve TINYINT(1) NOT NULL DEFAULT 0',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'allow_ai_direct_approve'
);
PREPARE add_ai_review_configs_allow_ai_direct_approve_stmt FROM @add_ai_review_configs_allow_ai_direct_approve;
EXECUTE add_ai_review_configs_allow_ai_direct_approve_stmt;
DEALLOCATE PREPARE add_ai_review_configs_allow_ai_direct_approve_stmt;

SET @add_ai_review_configs_allow_ai_direct_reject = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN allow_ai_direct_reject TINYINT(1) NOT NULL DEFAULT 0',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'allow_ai_direct_reject'
);
PREPARE add_ai_review_configs_allow_ai_direct_reject_stmt FROM @add_ai_review_configs_allow_ai_direct_reject;
EXECUTE add_ai_review_configs_allow_ai_direct_reject_stmt;
DEALLOCATE PREPARE add_ai_review_configs_allow_ai_direct_reject_stmt;

SET @add_ai_review_configs_reject_threshold = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN reject_threshold DECIMAL(5,2) DEFAULT NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'reject_threshold'
);
PREPARE add_ai_review_configs_reject_threshold_stmt FROM @add_ai_review_configs_reject_threshold;
EXECUTE add_ai_review_configs_reject_threshold_stmt;
DEALLOCATE PREPARE add_ai_review_configs_reject_threshold_stmt;

SET @add_ai_review_configs_confidence_threshold = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN confidence_threshold DECIMAL(3,2) DEFAULT 0.85',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'confidence_threshold'
);
PREPARE add_ai_review_configs_confidence_threshold_stmt FROM @add_ai_review_configs_confidence_threshold;
EXECUTE add_ai_review_configs_confidence_threshold_stmt;
DEALLOCATE PREPARE add_ai_review_configs_confidence_threshold_stmt;

SET @add_ai_review_configs_risk_flags_force_manual = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_configs ADD COLUMN risk_flags_force_manual JSON DEFAULT NULL COMMENT ''Risk flag values that force manual review''',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND column_name = 'risk_flags_force_manual'
);
PREPARE add_ai_review_configs_risk_flags_force_manual_stmt FROM @add_ai_review_configs_risk_flags_force_manual;
EXECUTE add_ai_review_configs_risk_flags_force_manual_stmt;
DEALLOCATE PREPARE add_ai_review_configs_risk_flags_force_manual_stmt;

SET @add_ai_review_results_flow_action = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_results ADD COLUMN flow_action VARCHAR(30) DEFAULT NULL COMMENT ''AI_DIRECT_APPROVE, AI_DIRECT_REJECT, AI_ASSIGN_MANUAL_REVIEW''',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_results'
      AND column_name = 'flow_action'
);
PREPARE add_ai_review_results_flow_action_stmt FROM @add_ai_review_results_flow_action;
EXECUTE add_ai_review_results_flow_action_stmt;
DEALLOCATE PREPARE add_ai_review_results_flow_action_stmt;

SET @add_ai_review_results_confidence = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE ai_review_results ADD COLUMN confidence DECIMAL(3,2) DEFAULT NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_results'
      AND column_name = 'confidence'
);
PREPARE add_ai_review_results_confidence_stmt FROM @add_ai_review_results_confidence;
EXECUTE add_ai_review_results_confidence_stmt;
DEALLOCATE PREPARE add_ai_review_results_confidence_stmt;
