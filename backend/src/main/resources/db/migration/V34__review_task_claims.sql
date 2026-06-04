-- 审核员对 (任务, 审核级别) 的整任务领取记录，唯一约束保证排他。
CREATE TABLE IF NOT EXISTS review_task_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    review_level INT NOT NULL DEFAULT 1,
    reviewer_id BIGINT NOT NULL,
    claimed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_review_task_claim (task_id, review_level),
    KEY idx_review_task_claims_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Reviewer whole-task claim per (task, review_level); exclusive.';
