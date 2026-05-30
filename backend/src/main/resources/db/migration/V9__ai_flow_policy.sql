-- AI auto-review flow policy support
ALTER TABLE ai_review_configs
    ADD COLUMN ai_flow_policy VARCHAR(30) NOT NULL DEFAULT 'MANUAL_FIRST'
        COMMENT 'MANUAL_FIRST, AI_PASS_ONLY, AI_REJECT_ONLY, AI_PASS_AND_REJECT, ALWAYS_MANUAL',
    ADD COLUMN allow_ai_direct_approve TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN allow_ai_direct_reject TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN reject_threshold DECIMAL(5,2) DEFAULT NULL,
    ADD COLUMN confidence_threshold DECIMAL(3,2) DEFAULT 0.85,
    ADD COLUMN risk_flags_force_manual JSON DEFAULT NULL
        COMMENT 'Risk flag values that force manual review';

ALTER TABLE ai_review_results
    ADD COLUMN flow_action VARCHAR(30) DEFAULT NULL
        COMMENT 'AI_DIRECT_APPROVE, AI_DIRECT_REJECT, AI_ASSIGN_MANUAL_REVIEW',
    ADD COLUMN confidence DECIMAL(3,2) DEFAULT NULL;
