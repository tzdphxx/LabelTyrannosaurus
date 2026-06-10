-- Complete multimodal pre-annotation observability and state model.
ALTER TABLE pre_annotations
    ADD COLUMN ignored_fields_json JSON NULL AFTER degraded,
    ADD COLUMN media_understanding_json JSON NULL AFTER ignored_fields_json;

SET @drop_pre_annotations_status_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE pre_annotations DROP CHECK chk_pre_annotations_status')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'pre_annotations'
      AND constraint_name = 'chk_pre_annotations_status'
);
PREPARE drop_pre_annotations_status_check_stmt FROM @drop_pre_annotations_status_check;
EXECUTE drop_pre_annotations_status_check_stmt;
DEALLOCATE PREPARE drop_pre_annotations_status_check_stmt;

ALTER TABLE pre_annotations
    ADD CONSTRAINT chk_pre_annotations_status
        CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED','RATE_LIMITED','MANUAL_REQUIRED'));
