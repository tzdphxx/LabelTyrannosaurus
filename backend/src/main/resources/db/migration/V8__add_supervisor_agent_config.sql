ALTER TABLE ai_review_configs
  ADD COLUMN agent_mode VARCHAR(24) NOT NULL DEFAULT 'DIRECT'
    COMMENT 'DIRECT | SUPERVISOR',
  ADD COLUMN enabled_tools JSON NULL
    COMMENT 'Enabled tool names list, null means all enabled',
  ADD COLUMN max_iterations INT NOT NULL DEFAULT 10,
  ADD CONSTRAINT chk_agent_mode CHECK (agent_mode IN ('DIRECT', 'SUPERVISOR'));
