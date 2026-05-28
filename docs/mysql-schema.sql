CREATE DATABASE IF NOT EXISTS labelhub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE labelhub;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS export_jobs;
DROP TABLE IF EXISTS labeler_task_stats;
DROP TABLE IF EXISTS labeler_daily_stats;
DROP TABLE IF EXISTS labeler_contribution_stats;
DROP TABLE IF EXISTS reward_ledger;
DROP TABLE IF EXISTS reward_rules;
DROP TABLE IF EXISTS conflict_groups;
DROP TABLE IF EXISTS review_records;
DROP TABLE IF EXISTS review_tasks;
DROP TABLE IF EXISTS ai_review_results;
DROP TABLE IF EXISTS agent_runs;
DROP TABLE IF EXISTS ai_review_configs;
DROP TABLE IF EXISTS submissions;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS template_versions;
DROP TABLE IF EXISTS templates;
DROP TABLE IF EXISTS dataset_item_change_logs;
DROP TABLE IF EXISTS dataset_import_jobs;
DROP TABLE IF EXISTS dataset_files;
DROP TABLE IF EXISTS dataset_items;
DROP TABLE IF EXISTS task_stats;
DROP TABLE IF EXISTS task_tags;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS llm_providers;
DROP TABLE IF EXISTS object_files;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NULL,
  user_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  login_enabled TINYINT(1) NOT NULL DEFAULT 1,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  token_version INT NOT NULL DEFAULT 1,
  display_name VARCHAR(100) NULL,
  avatar_url VARCHAR(500) NULL,
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_enabled (enabled),
  CONSTRAINT chk_users_user_type CHECK (user_type IN ('USER', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_roles_user_role (user_id, role_code),
  KEY idx_user_roles_role (role_code),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_user_roles_code CHECK (role_code IN ('ADMIN', 'OWNER', 'LABELER', 'REVIEWER', 'SYSTEM_AGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE object_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_id BIGINT NULL,
  bucket_name VARCHAR(128) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  checksum VARCHAR(128) NULL,
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'MINIO',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_object_files_object (bucket_name, object_key),
  KEY idx_object_files_owner (owner_id),
  CONSTRAINT fk_object_files_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE llm_providers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_code VARCHAR(64) NOT NULL,
  provider_name VARCHAR(100) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  encrypted_api_key TEXT NULL,
  default_model VARCHAR(128) NOT NULL,
  custom_headers_json JSON NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  platform_rate_limit_per_minute INT NULL,
  task_rate_limit_per_minute INT NULL,
  user_rate_limit_per_minute INT NULL,
  created_by BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_llm_providers_code (provider_code),
  KEY idx_llm_providers_enabled (enabled),
  CONSTRAINT fk_llm_providers_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  instruction_rich_text MEDIUMTEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  quota INT NOT NULL,
  claimed_count INT NOT NULL DEFAULT 0,
  overlap_count INT NOT NULL DEFAULT 1,
  deadline_at DATETIME(3) NOT NULL,
  published_template_version_id BIGINT NULL,
  ai_review_config_id BIGINT NULL COMMENT 'Current AI review config used when the task is published or submitted.',
  reward_visible TINYINT(1) NOT NULL DEFAULT 1,
  published_at DATETIME(3) NULL,
  ended_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_tasks_owner_status (owner_id, status),
  KEY idx_tasks_status_deadline (status, deadline_at),
  CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  CONSTRAINT chk_tasks_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'ENDED')),
  CONSTRAINT chk_tasks_quota CHECK (quota > 0),
  CONSTRAINT chk_tasks_overlap CHECK (overlap_count >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Task lifecycle and publish-time references.';

CREATE TABLE task_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  tag_name VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_task_tags_task_tag (task_id, tag_name),
  CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE task_stats (
  task_id BIGINT PRIMARY KEY,
  item_count INT NOT NULL DEFAULT 0,
  assigned_count INT NOT NULL DEFAULT 0,
  submitted_count INT NOT NULL DEFAULT 0,
  pending_review_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  conflict_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_task_stats_task FOREIGN KEY (task_id) REFERENCES tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dataset_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  dataset_type VARCHAR(40) NOT NULL,
  item_json JSON NOT NULL COMMENT 'Normalized dataset payload, independent from original JSON/JSONL/Excel file format.',
  metadata_json JSON NULL,
  assigned_count INT NOT NULL DEFAULT 0,
  submitted_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_dataset_items_task_external (task_id, external_id),
  KEY idx_dataset_items_claim (task_id, deleted, assigned_count),
  KEY idx_dataset_items_type (task_id, dataset_type),
  CONSTRAINT fk_dataset_items_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT chk_dataset_items_type CHECK (dataset_type IN ('QA_QUALITY', 'PREFERENCE_COMPARE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dataset items available for labeler claim and review.';

CREATE TABLE dataset_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  file_format VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_dataset_files_task (task_id),
  CONSTRAINT fk_dataset_files_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_dataset_files_file FOREIGN KEY (file_id) REFERENCES object_files(id),
  CONSTRAINT fk_dataset_files_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_dataset_files_format CHECK (file_format IN ('JSON', 'JSONL', 'EXCEL', 'CSV'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dataset_import_jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  dataset_file_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  import_mode VARCHAR(20) NOT NULL DEFAULT 'APPEND',
  total_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  error_report_file_id BIGINT NULL,
  error_message TEXT NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_dataset_import_jobs_task (task_id, status),
  CONSTRAINT fk_import_jobs_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_import_jobs_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_files(id),
  CONSTRAINT fk_import_jobs_error_file FOREIGN KEY (error_report_file_id) REFERENCES object_files(id),
  CONSTRAINT fk_import_jobs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_import_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL_SUCCESS')),
  CONSTRAINT chk_import_jobs_mode CHECK (import_mode IN ('APPEND', 'OVERWRITE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dataset_item_change_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  item_id BIGINT NULL,
  change_type VARCHAR(32) NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  json_patch JSON NULL,
  actor_id BIGINT NOT NULL,
  failure_reason TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_item_change_logs_task (task_id, created_at),
  KEY idx_item_change_logs_item (item_id),
  CONSTRAINT fk_item_change_logs_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_item_change_logs_item FOREIGN KEY (item_id) REFERENCES dataset_items(id),
  CONSTRAINT fk_item_change_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  current_version_no INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_templates_task (task_id),
  CONSTRAINT fk_templates_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_templates_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE template_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  schema_json JSON NOT NULL COMMENT 'Frozen renderer schema: layout, components, ShowItem bindings, LlmTrigger config, and validation rules.',
  published_snapshot TINYINT(1) NOT NULL DEFAULT 0,
  change_note VARCHAR(500) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_template_versions_template_version (template_id, version_no),
  KEY idx_template_versions_task (task_id),
  CONSTRAINT fk_template_versions_template FOREIGN KEY (template_id) REFERENCES templates(id),
  CONSTRAINT fk_template_versions_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_template_versions_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable template schema versions used by owner preview, labeler rendering, and backend validation.';

CREATE TABLE assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  dataset_item_id BIGINT NOT NULL,
  labeler_id BIGINT NOT NULL,
  template_version_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CLAIMED',
  draft_answer_json JSON NULL COMMENT 'Latest server-side draft answer; Redis may cache the same draft for faster autosave.',
  draft_version INT NOT NULL DEFAULT 1,
  claimed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  submitted_at DATETIME(3) NULL,
  returned_at DATETIME(3) NULL,
  ai_returned_at DATETIME(3) NULL,
  approved_at DATETIME(3) NULL,
  cancelled_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_assignments_item_labeler (dataset_item_id, labeler_id),
  KEY idx_assignments_labeler_status (labeler_id, status),
  KEY idx_assignments_task_status (task_id, status),
  CONSTRAINT fk_assignments_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_assignments_item FOREIGN KEY (dataset_item_id) REFERENCES dataset_items(id),
  CONSTRAINT fk_assignments_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT fk_assignments_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions(id),
  CONSTRAINT chk_assignments_status CHECK (status IN ('CLAIMED', 'DRAFTING', 'SUBMITTED', 'AI_RETURNED', 'RETURNED', 'APPROVED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Claim record and draft state for one labeler on one dataset item.';

CREATE TABLE submissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  dataset_item_id BIGINT NOT NULL,
  labeler_id BIGINT NOT NULL,
  template_version_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  answer_json JSON NOT NULL,
  answer_hash CHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  conflict_status VARCHAR(24) NOT NULL DEFAULT 'NONE',
  current_review_level INT NOT NULL DEFAULT 1 COMMENT 'Current human review stage: 1 initial review, 2 second review, 3 final review.',
  review_flow_status VARCHAR(24) NOT NULL DEFAULT 'UNASSIGNED' COMMENT 'Human review queue state, separate from submission business status.',
  assigned_reviewer_id BIGINT NULL COMMENT 'Current reviewer owner for "assigned to me" filtering; history is stored in review_tasks/review_records.',
  review_version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version for reviewer state transitions.',
  is_golden TINYINT(1) NOT NULL DEFAULT 0,
  golden_dataset_item_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_golden = 1 THEN dataset_item_id ELSE NULL END) STORED,
  submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_submissions_assignment_version (assignment_id, version_no),
  UNIQUE KEY uk_submissions_golden_item (golden_dataset_item_id),
  KEY idx_submissions_review (status, conflict_status, submitted_at),
  KEY idx_submissions_assigned_reviewer (assigned_reviewer_id, review_flow_status, status),
  KEY idx_submissions_task_item (task_id, dataset_item_id),
  KEY idx_submissions_labeler (labeler_id, status),
  CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
  CONSTRAINT fk_submissions_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_submissions_item FOREIGN KEY (dataset_item_id) REFERENCES dataset_items(id),
  CONSTRAINT fk_submissions_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT fk_submissions_assigned_reviewer FOREIGN KEY (assigned_reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_submissions_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions(id),
  CONSTRAINT chk_submissions_status CHECK (status IN ('SUBMITTED', 'AI_REVIEWING', 'AI_REJECTED', 'PENDING_FINAL', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
  CONSTRAINT chk_submissions_conflict_status CHECK (conflict_status IN ('NONE', 'CONSENSUS_REACHED', 'CONFLICTED', 'RESOLVED')),
  CONSTRAINT chk_submissions_review_flow CHECK (review_flow_status IN ('UNASSIGNED', 'ASSIGNED', 'IN_REVIEW', 'LEVEL_APPROVED', 'FINAL_APPROVED', 'REJECTED', 'CANCELLED')),
  CONSTRAINT chk_submissions_review_level CHECK (current_review_level BETWEEN 1 AND 3),
  CONSTRAINT chk_submissions_review_version CHECK (review_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable submission versions and current review routing state.';

CREATE TABLE ai_review_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  provider_id BIGINT NULL,
  model_name VARCHAR(128) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  prompt_template MEDIUMTEXT NULL,
  output_schema_json JSON NULL,
  dimension_config_json JSON NULL,
  pass_threshold DECIMAL(5,2) NULL,
  reject_threshold DECIMAL(5,2) NULL,
  manual_threshold DECIMAL(5,2) NULL,
  ai_reject_action VARCHAR(24) NOT NULL DEFAULT 'SUGGEST_ONLY' COMMENT 'Whether AI reject only suggests, returns to labeler, or requires manual review.',
  max_retry INT NOT NULL DEFAULT 3,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_ai_review_configs_task (task_id),
  CONSTRAINT fk_ai_review_configs_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_ai_review_configs_provider FOREIGN KEY (provider_id) REFERENCES llm_providers(id),
  CONSTRAINT fk_ai_review_configs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_ai_review_configs_reject_action CHECK (ai_reject_action IN ('SUGGEST_ONLY', 'RETURN_TO_LABELER', 'MANUAL_REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Per-task AI review prompt, thresholds, provider, and reject routing policy.';

CREATE TABLE agent_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_type VARCHAR(32) NOT NULL,
  submission_id BIGINT NULL,
  provider_id BIGINT NULL,
  model_name VARCHAR(128) NULL,
  prompt_version VARCHAR(64) NULL,
  input_snapshot JSON NULL COMMENT 'Prompt input snapshot for traceability; do not reconstruct it from mutable business tables.',
  output_snapshot JSON NULL COMMENT 'Raw or normalized model output for this concrete agent execution.',
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  error_message TEXT NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_agent_runs_submission (submission_id, created_at),
  KEY idx_agent_runs_status (status, created_at),
  CONSTRAINT fk_agent_runs_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_agent_runs_provider FOREIGN KEY (provider_id) REFERENCES llm_providers(id),
  CONSTRAINT chk_agent_runs_type CHECK (agent_type IN ('AI_REVIEW', 'LLM_TRIGGER')),
  CONSTRAINT chk_agent_runs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'RATE_LIMITED', 'MANUAL_REQUIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Concrete AI/LLM execution attempts, including retries and failures.';

CREATE TABLE ai_review_results (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  effective_run_id BIGINT NULL COMMENT 'The agent run whose output is currently effective for this AI review result.',
  provider_id BIGINT NULL,
  model_name VARCHAR(128) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  decision VARCHAR(24) NULL,
  average_score DECIMAL(6,3) NULL,
  dimension_scores JSON NULL,
  risk_flags JSON NULL,
  suggestion TEXT NULL,
  prompt_snapshot MEDIUMTEXT NULL COMMENT 'Final prompt sent to the provider for audit display.',
  raw_response MEDIUMTEXT NULL COMMENT 'Original provider response retained for troubleshooting and review transparency.',
  retry_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_ai_review_results_submission (submission_id),
  UNIQUE KEY uk_ai_review_results_effective_run (effective_run_id),
  KEY idx_ai_review_results_status (status),
  CONSTRAINT fk_ai_review_results_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_ai_review_results_effective_run FOREIGN KEY (effective_run_id) REFERENCES agent_runs(id),
  CONSTRAINT fk_ai_review_results_provider FOREIGN KEY (provider_id) REFERENCES llm_providers(id),
  CONSTRAINT chk_ai_review_results_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'RATE_LIMITED', 'MANUAL_REQUIRED')),
  CONSTRAINT chk_ai_review_results_decision CHECK (decision IS NULL OR decision IN ('PASS', 'REJECT', 'MANUAL_REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Business-level AI review conclusion for a submission; agent_runs stores execution details.';

CREATE TABLE review_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  review_level INT NOT NULL DEFAULT 1,
  assigned_reviewer_id BIGINT NOT NULL,
  assigned_by BIGINT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  review_version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version for concurrent reviewer actions.',
  assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  started_at DATETIME(3) NULL,
  completed_at DATETIME(3) NULL,
  due_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_review_tasks_submission_level (submission_id, review_level),
  KEY idx_review_tasks_reviewer_status (assigned_reviewer_id, status, due_at),
  KEY idx_review_tasks_task_level (task_id, review_level, status),
  CONSTRAINT fk_review_tasks_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_review_tasks_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_review_tasks_assigned_reviewer FOREIGN KEY (assigned_reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_review_tasks_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
  CONSTRAINT chk_review_tasks_level CHECK (review_level BETWEEN 1 AND 3),
  CONSTRAINT chk_review_tasks_status CHECK (status IN ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'TRANSFERRED', 'CANCELLED')),
  CONSTRAINT chk_review_tasks_version CHECK (review_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Reviewer work queue for batch assignment and level 1/2/3 review flow.';

CREATE TABLE review_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  review_task_id BIGINT NULL COMMENT 'Review queue item this action belongs to; nullable for legacy/direct audit actions.',
  reviewer_id BIGINT NOT NULL,
  target_reviewer_id BIGINT NULL COMMENT 'Reviewer assigned by ASSIGN_REVIEWER action; reviewer_id is the actor who performed the assignment.',
  action VARCHAR(32) NOT NULL,
  review_level INT NOT NULL DEFAULT 1,
  comment TEXT NULL,
  reason TEXT NULL,
  before_status VARCHAR(20) NULL,
  after_status VARCHAR(20) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_review_records_submission (submission_id, created_at),
  KEY idx_review_records_reviewer (reviewer_id, created_at),
  KEY idx_review_records_target_reviewer (target_reviewer_id, created_at),
  CONSTRAINT fk_review_records_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_review_records_review_task FOREIGN KEY (review_task_id) REFERENCES review_tasks(id),
  CONSTRAINT fk_review_records_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_review_records_target_reviewer FOREIGN KEY (target_reviewer_id) REFERENCES users(id),
  CONSTRAINT chk_review_records_action CHECK (action IN ('APPROVE', 'REJECT', 'RESOLVE_CONFLICT', 'MARK_MANUAL_REQUIRED', 'ASSIGN_REVIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only human review actions and assignment audit records.';

CREATE TABLE conflict_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  dataset_item_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'CONFLICTED',
  consensus_score DECIMAL(6,3) NULL COMMENT 'Calculated agreement score across overlapping submissions.',
  golden_submission_id BIGINT NULL COMMENT 'Reviewer-selected golden submission for conflicted or overlapping labels.',
  resolved_by BIGINT NULL,
  resolved_reason TEXT NULL,
  resolved_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_conflict_groups_task_item (task_id, dataset_item_id),
  KEY idx_conflict_groups_status (status),
  CONSTRAINT fk_conflict_groups_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_conflict_groups_item FOREIGN KEY (dataset_item_id) REFERENCES dataset_items(id),
  CONSTRAINT fk_conflict_groups_golden_submission FOREIGN KEY (golden_submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_conflict_groups_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(id),
  CONSTRAINT chk_conflict_groups_status CHECK (status IN ('NONE', 'CONSENSUS_REACHED', 'CONFLICTED', 'RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Conflict and consensus resolution for multi-labeler overlap.';

CREATE TABLE reward_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  effective_version INT NOT NULL COMMENT 'Reward rule version effective for newly approved data; historical ledgers are not rewritten.',
  reward_mode VARCHAR(32) NOT NULL DEFAULT 'APPROVED_ITEM',
  unit_reward DECIMAL(12,2) NOT NULL DEFAULT 0,
  reward_currency VARCHAR(32) NOT NULL DEFAULT 'POINT',
  reward_visible TINYINT(1) NOT NULL DEFAULT 1,
  effective_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_reward_rules_task_version (task_id, effective_version),
  KEY idx_reward_rules_task (task_id),
  CONSTRAINT fk_reward_rules_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_reward_rules_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_reward_rules_mode CHECK (reward_mode IN ('APPROVED_ITEM')),
  CONSTRAINT chk_reward_rules_unit_reward CHECK (unit_reward >= 0),
  CONSTRAINT chk_reward_rules_effective_version CHECK (effective_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Versioned virtual reward configuration for a task.';

CREATE TABLE reward_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  labeler_id BIGINT NOT NULL,
  submission_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  direction VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  source_event_id VARCHAR(128) NOT NULL,
  reward_type VARCHAR(32) NOT NULL DEFAULT 'SUBMISSION_APPROVED',
  positive_submission_id BIGINT GENERATED ALWAYS AS (CASE WHEN direction = 'CREDIT' THEN submission_id ELSE NULL END) STORED COMMENT 'Idempotency key: one positive reward per submission.',
  positive_assignment_id BIGINT GENERATED ALWAYS AS (CASE WHEN direction = 'CREDIT' THEN assignment_id ELSE NULL END) STORED COMMENT 'Idempotency key: one effective positive reward per assignment across versions.',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_reward_ledger_event (source_event_id),
  UNIQUE KEY uk_reward_ledger_positive_submission (positive_submission_id),
  UNIQUE KEY uk_reward_ledger_positive_assignment (positive_assignment_id),
  KEY idx_reward_ledger_labeler (labeler_id, created_at),
  KEY idx_reward_ledger_task (task_id, created_at),
  CONSTRAINT fk_reward_ledger_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_reward_ledger_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT fk_reward_ledger_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
  CONSTRAINT fk_reward_ledger_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
  CONSTRAINT chk_reward_ledger_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
  CONSTRAINT chk_reward_ledger_amount CHECK (amount > 0),
  CONSTRAINT chk_reward_ledger_type CHECK (reward_type IN ('SUBMISSION_APPROVED', 'GOLDEN_SELECTED', 'REWARD_REVERSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only virtual reward ledger; reversals are negative rows, not updates.';

CREATE TABLE labeler_contribution_stats (
  labeler_id BIGINT PRIMARY KEY,
  claimed_count INT NOT NULL DEFAULT 0,
  submitted_count INT NOT NULL DEFAULT 0,
  pending_review_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  total_reward DECIMAL(14,2) NOT NULL DEFAULT 0,
  today_submitted_count INT NOT NULL DEFAULT 0,
  last_submit_date DATE NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_contribution_stats_labeler FOREIGN KEY (labeler_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE labeler_daily_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  labeler_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  submitted_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  reward_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_labeler_daily_stats_date (labeler_id, stat_date),
  KEY idx_labeler_daily_stats_date (stat_date),
  CONSTRAINT fk_labeler_daily_stats_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT chk_labeler_daily_stats_counts CHECK (
    submitted_count >= 0
    AND approved_count >= 0
    AND rejected_count >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Daily labeler contribution snapshot for trend charts.';

CREATE TABLE labeler_task_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  labeler_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  claimed_count INT NOT NULL DEFAULT 0,
  submitted_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  total_reward DECIMAL(14,2) NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_labeler_task_stats (labeler_id, task_id),
  KEY idx_labeler_task_stats_task (task_id),
  CONSTRAINT fk_labeler_task_stats_labeler FOREIGN KEY (labeler_id) REFERENCES users(id),
  CONSTRAINT fk_labeler_task_stats_task FOREIGN KEY (task_id) REFERENCES tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE export_jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  requested_by BIGINT NOT NULL,
  export_format VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  include_ai_review TINYINT(1) NOT NULL DEFAULT 0,
  include_audit_trail TINYINT(1) NOT NULL DEFAULT 0,
  include_review_comment TINYINT(1) NOT NULL DEFAULT 0,
  include_labeler_info TINYINT(1) NOT NULL DEFAULT 0,
  field_mapping_json JSON NULL COMMENT 'Export field mapping array: source JSONPath, target name, formatter, and include flags.',
  result_file_id BIGINT NULL,
  download_url VARCHAR(1000) NULL,
  error_message TEXT NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_export_jobs_task (task_id, created_at),
  KEY idx_export_jobs_status (status),
  CONSTRAINT fk_export_jobs_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_export_jobs_requested_by FOREIGN KEY (requested_by) REFERENCES users(id),
  CONSTRAINT fk_export_jobs_result_file FOREIGN KEY (result_file_id) REFERENCES object_files(id),
  CONSTRAINT chk_export_jobs_format CHECK (export_format IN ('JSON', 'JSONL', 'CSV', 'EXCEL')),
  CONSTRAINT chk_export_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Async export configuration and output file metadata.';

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  actor_type VARCHAR(32) NOT NULL,
  actor_id BIGINT NULL,
  action VARCHAR(64) NOT NULL,
  before_json JSON NULL COMMENT 'Business object snapshot before the action when available.',
  after_json JSON NULL COMMENT 'Business object snapshot after the action when available.',
  trace_id VARCHAR(128) NULL,
  agent_run_id BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_audit_logs_biz (biz_type, biz_id, created_at),
  KEY idx_audit_logs_actor (actor_id, created_at),
  KEY idx_audit_logs_trace (trace_id),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  CONSTRAINT fk_audit_logs_agent_run FOREIGN KEY (agent_run_id) REFERENCES agent_runs(id),
  CONSTRAINT chk_audit_logs_actor_type CHECK (actor_type IN ('USER', 'SYSTEM_AGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only audit trail for state transitions and critical business operations.';

ALTER TABLE tasks
  ADD CONSTRAINT fk_tasks_published_template_version
    FOREIGN KEY (published_template_version_id) REFERENCES template_versions(id),
  ADD CONSTRAINT fk_tasks_ai_review_config
    FOREIGN KEY (ai_review_config_id) REFERENCES ai_review_configs(id);

CREATE INDEX idx_tasks_template_version ON tasks (published_template_version_id);
CREATE INDEX idx_tasks_ai_review_config ON tasks (ai_review_config_id);
CREATE INDEX idx_task_tags_tag_task ON task_tags (tag_name, task_id);
CREATE FULLTEXT INDEX ft_tasks_search ON tasks (title, description);
CREATE INDEX idx_submissions_task_review ON submissions (task_id, status, conflict_status, submitted_at);
CREATE INDEX idx_submissions_export ON submissions (task_id, status, is_golden, submitted_at);
CREATE INDEX idx_submissions_labeler_time ON submissions (labeler_id, submitted_at);
CREATE INDEX idx_ai_review_results_decision_status ON ai_review_results (decision, status);
CREATE INDEX idx_audit_logs_agent_run ON audit_logs (agent_run_id, created_at);
CREATE INDEX idx_export_jobs_requested_by ON export_jobs (requested_by, created_at);
