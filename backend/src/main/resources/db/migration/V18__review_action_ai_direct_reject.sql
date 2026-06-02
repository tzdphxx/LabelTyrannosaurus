-- Align persisted status/action values with the API contract wording.
UPDATE submissions
SET status = 'PENDING_FINAL'
WHERE status = 'AI_REJECTED';

UPDATE review_records
SET action = 'AI_DIRECT_REJECT'
WHERE action = 'AI_REJECT';

SET @drop_submissions_status_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE submissions DROP CHECK chk_submissions_status')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'submissions'
      AND constraint_name = 'chk_submissions_status'
);
PREPARE drop_submissions_status_check_stmt FROM @drop_submissions_status_check;
EXECUTE drop_submissions_status_check_stmt;
DEALLOCATE PREPARE drop_submissions_status_check_stmt;

ALTER TABLE submissions
    ADD CONSTRAINT chk_submissions_status
        CHECK (status IN ('SUBMITTED', 'AI_REVIEWING', 'PENDING_FINAL',
                          'APPROVED', 'REJECTED', 'SUPERSEDED'));

SET @drop_review_action_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE review_records DROP CHECK chk_review_records_action')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'review_records'
      AND constraint_name = 'chk_review_records_action'
);
PREPARE drop_review_action_check_stmt FROM @drop_review_action_check;
EXECUTE drop_review_action_check_stmt;
DEALLOCATE PREPARE drop_review_action_check_stmt;

ALTER TABLE review_records
    ADD CONSTRAINT chk_review_records_action
        CHECK (action IN ('APPROVE', 'REJECT', 'AI_DIRECT_REJECT',
                          'RESOLVE_CONFLICT', 'MARK_MANUAL_REQUIRED', 'ASSIGN_REVIEWER'));
