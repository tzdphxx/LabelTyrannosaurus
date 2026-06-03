-- V35: Add UNIQUE constraint on ai_review_configs.task_id
-- Prevents duplicate AI review configs for the same task at DB level.
-- Before adding the constraint, deduplicate by keeping the latest row (max id).

DELETE FROM ai_review_configs
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MAX(id) AS id FROM ai_review_configs GROUP BY task_id
    ) AS kept
);

ALTER TABLE ai_review_configs
    ADD UNIQUE KEY `uk_ai_review_configs_task` (`task_id`);
