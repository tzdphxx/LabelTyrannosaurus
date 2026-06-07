ALTER TABLE tasks
    ADD COLUMN strategy VARCHAR(20) NOT NULL DEFAULT 'FCFS'
        COMMENT '领取策略: FCFS/QUOTA_GRAB/ASSIGNED',
    ADD COLUMN max_claims_per_labeler INT NULL
        COMMENT '单人并发未完成上限(仅 QUOTA_GRAB 有效)';

CREATE TABLE assignment_dispatches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    dataset_item_id BIGINT NOT NULL,
    labeler_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CLAIMED/EXPIRED/REVOKED',
    dispatched_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    claimed_at DATETIME(3) NULL,
    expires_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_dispatch_labeler_task (labeler_id, task_id, status),
    UNIQUE KEY uk_dispatch_pending_item (task_id, dataset_item_id, status),
    CONSTRAINT fk_dispatch_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_dispatch_labeler FOREIGN KEY (labeler_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Owner 手动指派标注任务';
