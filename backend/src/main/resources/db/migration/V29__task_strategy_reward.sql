-- V29: task strategy + embedded reward, remove overlap_count / reward_visible / reward_rules
ALTER TABLE tasks
    ADD COLUMN strategy VARCHAR(20) NOT NULL DEFAULT 'FCFS' AFTER review_level_count,
    ADD COLUMN reward_per_approval DECIMAL(10, 2) NULL AFTER strategy,
    ADD COLUMN penalty_per_rejection DECIMAL(10, 2) NULL AFTER reward_per_approval,
    ADD COLUMN bonus_threshold INT NULL AFTER penalty_per_rejection,
    ADD COLUMN bonus_points DECIMAL(10, 2) NULL AFTER bonus_threshold,
    DROP COLUMN overlap_count,
    DROP COLUMN reward_visible;

DROP TABLE IF EXISTS reward_rules;
