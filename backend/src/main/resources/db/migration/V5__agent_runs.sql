CREATE TABLE agent_runs (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  agent_type      VARCHAR(64)  NOT NULL,
  submission_id   BIGINT       NOT NULL,
  provider_id     BIGINT       NULL,
  model_name      VARCHAR(128) NULL,
  prompt_version  VARCHAR(64)  NULL,
  input_snapshot  JSON         NULL,
  output_snapshot JSON         NULL,
  status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
  error_message   TEXT         NULL,
  started_at      DATETIME(3)  NULL,
  finished_at     DATETIME(3)  NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_agent_runs_submission (submission_id),
  KEY idx_agent_runs_status (status),
  CONSTRAINT chk_agent_runs_status CHECK (
    status IN ('PENDING','RUNNING','SUCCESS','FAILED','RATE_LIMITED','MANUAL_REQUIRED')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='AI agent execution runs — one row per invocation attempt.';
