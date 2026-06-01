SET @drop_conflict_groups_status_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE conflict_groups DROP CHECK chk_conflict_groups_status')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'conflict_groups'
      AND constraint_name = 'chk_conflict_groups_status'
);
PREPARE drop_conflict_groups_status_check_stmt FROM @drop_conflict_groups_status_check;
EXECUTE drop_conflict_groups_status_check_stmt;
DEALLOCATE PREPARE drop_conflict_groups_status_check_stmt;

UPDATE conflict_groups
SET status = CASE
    WHEN status = 'CONFLICTED' THEN 'OPEN'
    WHEN status IN ('NONE', 'CONSENSUS_REACHED') THEN 'RESOLVED'
    ELSE status
END
WHERE status IN ('NONE', 'CONSENSUS_REACHED', 'CONFLICTED');

ALTER TABLE conflict_groups
    MODIFY COLUMN status VARCHAR(24) NOT NULL DEFAULT 'OPEN';

ALTER TABLE conflict_groups
    ADD CONSTRAINT chk_conflict_groups_status
        CHECK (status IN ('OPEN', 'RESOLVED'));

SET @drop_review_records_action_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE review_records DROP CHECK chk_review_records_action')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'review_records'
      AND constraint_name = 'chk_review_records_action'
);
PREPARE drop_review_records_action_check_stmt FROM @drop_review_records_action_check;
EXECUTE drop_review_records_action_check_stmt;
DEALLOCATE PREPARE drop_review_records_action_check_stmt;

ALTER TABLE review_records
    ADD CONSTRAINT chk_review_records_action
        CHECK (action IN ('APPROVE', 'REJECT', 'AI_REJECT', 'RESOLVE_CONFLICT',
                          'MARK_MANUAL_REQUIRED', 'ASSIGN_REVIEWER'));
