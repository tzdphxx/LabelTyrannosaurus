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
  KEY idx_tasks_template_version (published_template_version_id),
  KEY idx_tasks_ai_review_config (ai_review_config_id),
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
  KEY idx_task_tags_tag_task (tag_name, task_id),
  CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dataset_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  dataset_type VARCHAR(40) NOT NULL,
  item_json JSON NOT NULL COMMENT 'Normalized dataset payload used by labeler claim.',
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
  CONSTRAINT chk_dataset_items_type CHECK (dataset_type IN ('QA_QUALITY', 'PREFERENCE_COMPARE')),
  CONSTRAINT chk_dataset_items_assigned_count CHECK (assigned_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dataset items available for labeler claim and review.';

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
  schema_json JSON NOT NULL COMMENT 'Frozen renderer schema used by labeler rendering and backend validation.',
  published_snapshot TINYINT(1) NOT NULL DEFAULT 0,
  change_note VARCHAR(500) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_template_versions_template_version (template_id, version_no),
  KEY idx_template_versions_task (task_id),
  CONSTRAINT fk_template_versions_template FOREIGN KEY (template_id) REFERENCES templates(id),
  CONSTRAINT fk_template_versions_task FOREIGN KEY (task_id) REFERENCES tasks(id),
  CONSTRAINT fk_template_versions_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable template schema versions used by owner preview and labeler rendering.';

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
  CONSTRAINT chk_assignments_status CHECK (status IN ('CLAIMED', 'DRAFTING', 'SUBMITTED', 'AI_RETURNED', 'RETURNED', 'APPROVED', 'CANCELLED')),
  CONSTRAINT chk_assignments_draft_version CHECK (draft_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Claim record and draft state for one labeler on one dataset item.';

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
  KEY idx_audit_logs_agent_run (agent_run_id, created_at),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  CONSTRAINT chk_audit_logs_actor_type CHECK (actor_type IN ('USER', 'SYSTEM_AGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only audit trail for state transitions and critical business operations.';
