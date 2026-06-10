-- P2 Agent pre-annotation and multimodal AI support.
ALTER TABLE agent_runs
    MODIFY COLUMN submission_id BIGINT NULL,
    ADD COLUMN assignment_id BIGINT NULL AFTER submission_id,
    ADD KEY idx_agent_runs_assignment (assignment_id, created_at),
    ADD CONSTRAINT fk_agent_runs_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id);

SET @drop_agent_runs_type_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE agent_runs DROP CHECK chk_agent_runs_type')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'agent_runs'
      AND constraint_name = 'chk_agent_runs_type'
);
PREPARE drop_agent_runs_type_check_stmt FROM @drop_agent_runs_type_check;
EXECUTE drop_agent_runs_type_check_stmt;
DEALLOCATE PREPARE drop_agent_runs_type_check_stmt;

ALTER TABLE agent_runs
    ADD CONSTRAINT chk_agent_runs_type CHECK (agent_type IN ('AI_REVIEW','LLM_TRIGGER','AI_REVIEW_CONFIG_TEST','PRE_ANNOTATION'));

ALTER TABLE llm_providers
    ADD COLUMN support_vision TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN support_multi_image TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN max_image_count INT NOT NULL DEFAULT 10,
    ADD COLUMN vision_model VARCHAR(100) DEFAULT NULL;

ALTER TABLE ai_review_configs
    ADD COLUMN multimodal_enabled TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN degradation_penalty DECIMAL(3,2) DEFAULT 0.20,
    ADD COLUMN vision_detail VARCHAR(20) DEFAULT 'auto',
    ADD COLUMN max_images_per_request INT DEFAULT 5,
    ADD COLUMN allow_ai_direct_approve_when_degraded TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE ai_review_results
    ADD COLUMN prompt_mode VARCHAR(40) DEFAULT NULL,
    ADD COLUMN degraded TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN limitations JSON DEFAULT NULL;

CREATE TABLE pre_annotations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  dataset_item_id BIGINT NOT NULL,
  labeler_id BIGINT NOT NULL,
  agent_run_id BIGINT NULL,
  status VARCHAR(30) NOT NULL,
  suggested_answer_json JSON NULL,
  field_suggestions JSON NULL,
  risk_flags JSON NULL,
  overall_confidence DECIMAL(3,2) NULL,
  limitations JSON NULL,
  prompt_mode VARCHAR(40) NULL,
  degraded TINYINT(1) NOT NULL DEFAULT 0,
  raw_response LONGTEXT NULL,
  error_code VARCHAR(100) NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_pre_annotations_assignment (assignment_id, created_at),
  KEY idx_pre_annotations_agent_run (agent_run_id),
  CONSTRAINT fk_pre_annotations_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
  CONSTRAINT fk_pre_annotations_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_pre_annotations_item FOREIGN KEY (dataset_item_id) REFERENCES dataset_items(id),
  CONSTRAINT fk_pre_annotations_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT fk_pre_annotations_agent_run FOREIGN KEY (agent_run_id) REFERENCES agent_runs(id),
  CONSTRAINT chk_pre_annotations_status CHECK (status IN ('SUCCESS', 'FAILED', 'RATE_LIMITED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Immutable AI pre-annotation suggestions for labeler assignments.';
