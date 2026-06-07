-- Enforce one active labeler per dataset item and normalize task overlap to 1.
-- Idempotent for development databases that already received this change under
-- an older migration number.

UPDATE tasks
SET overlap_count = 1
WHERE overlap_count <> 1;

UPDATE assignments a
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY dataset_item_id
                   ORDER BY updated_at DESC, id DESC
               ) AS active_rank
        FROM assignments
        WHERE status <> 'CANCELLED'
    ) ranked
    WHERE active_rank > 1
) duplicates ON duplicates.id = a.id
SET a.status = 'CANCELLED',
    a.updated_at = CURRENT_TIMESTAMP(3);

UPDATE dataset_items di
LEFT JOIN (
    SELECT dataset_item_id, COUNT(*) AS active_assignment_count
    FROM assignments
    WHERE status <> 'CANCELLED'
    GROUP BY dataset_item_id
) active_assignments ON active_assignments.dataset_item_id = di.id
SET di.assigned_count = CASE
    WHEN COALESCE(active_assignments.active_assignment_count, 0) > 0 THEN 1
    ELSE 0
END;

SET @add_assignments_active_dataset_item_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE assignments ADD COLUMN active_dataset_item_id BIGINT GENERATED ALWAYS AS (CASE WHEN status <> ''CANCELLED'' THEN dataset_item_id ELSE NULL END) STORED',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'assignments'
      AND column_name = 'active_dataset_item_id'
);
PREPARE stmt FROM @add_assignments_active_dataset_item_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assignments_active_item_unique = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE assignments ADD CONSTRAINT uk_assignments_active_item UNIQUE (active_dataset_item_id)',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'assignments'
      AND constraint_name = 'uk_assignments_active_item'
);
PREPARE stmt FROM @add_assignments_active_item_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_tasks_overlap_single_check = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE tasks ADD CONSTRAINT chk_tasks_overlap_single CHECK (overlap_count = 1)',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'tasks'
      AND constraint_name = 'chk_tasks_overlap_single'
);
PREPARE stmt FROM @add_tasks_overlap_single_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
