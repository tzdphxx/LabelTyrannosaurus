ALTER TABLE conflict_groups
    DROP CHECK chk_conflict_groups_status;

ALTER TABLE conflict_groups
    MODIFY COLUMN status VARCHAR(24) NOT NULL DEFAULT 'OPEN';

ALTER TABLE conflict_groups
    ADD CONSTRAINT chk_conflict_groups_status
        CHECK (status IN ('OPEN', 'RESOLVED'));

ALTER TABLE review_records
    DROP CHECK chk_review_records_action;

ALTER TABLE review_records
    ADD CONSTRAINT chk_review_records_action
        CHECK (action IN ('APPROVE', 'REJECT', 'AI_REJECT', 'RESOLVE_CONFLICT',
                          'MARK_MANUAL_REQUIRED', 'ASSIGN_REVIEWER'));
