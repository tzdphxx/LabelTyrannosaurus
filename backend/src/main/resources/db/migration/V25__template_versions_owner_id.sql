SET @add_template_versions_owner_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE template_versions ADD COLUMN owner_id BIGINT NULL AFTER task_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND column_name = 'owner_id'
);
PREPARE stmt FROM @add_template_versions_owner_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE template_versions tv
JOIN tasks task ON task.id = tv.task_id
SET tv.owner_id = task.owner_id
WHERE tv.owner_id IS NULL;

SET @modify_template_versions_owner_id = (
    SELECT IF(is_nullable = 'YES',
        'ALTER TABLE template_versions MODIFY COLUMN owner_id BIGINT NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND column_name = 'owner_id'
);
PREPARE stmt FROM @modify_template_versions_owner_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_template_versions_owner_index = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE template_versions ADD KEY idx_template_versions_owner (owner_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND index_name = 'idx_template_versions_owner'
);
PREPARE stmt FROM @add_template_versions_owner_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_template_versions_owner_fk = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE template_versions ADD CONSTRAINT fk_template_versions_owner FOREIGN KEY (owner_id) REFERENCES users(id)',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND constraint_name = 'fk_template_versions_owner'
);
PREPARE stmt FROM @add_template_versions_owner_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
