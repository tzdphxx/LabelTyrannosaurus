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
