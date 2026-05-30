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
