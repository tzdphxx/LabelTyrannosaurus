-- Enforce one active labeler per dataset item and normalize task overlap to 1.

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

ALTER TABLE assignments
    ADD COLUMN active_dataset_item_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN status <> 'CANCELLED' THEN dataset_item_id ELSE NULL END
        ) STORED;

ALTER TABLE assignments
    ADD CONSTRAINT uk_assignments_active_item UNIQUE (active_dataset_item_id);

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_overlap_single CHECK (overlap_count = 1);
