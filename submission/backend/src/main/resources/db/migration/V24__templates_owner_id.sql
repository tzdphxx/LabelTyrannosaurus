SET @add_templates_owner_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE templates ADD COLUMN owner_id BIGINT NULL AFTER task_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND column_name = 'owner_id'
);
PREPARE stmt FROM @add_templates_owner_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE templates t
JOIN tasks task ON task.id = t.task_id
SET t.owner_id = task.owner_id
WHERE t.owner_id IS NULL;

SET @modify_templates_owner_id = (
    SELECT IF(is_nullable = 'YES',
        'ALTER TABLE templates MODIFY COLUMN owner_id BIGINT NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND column_name = 'owner_id'
);
PREPARE stmt FROM @modify_templates_owner_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_templates_owner_index = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE templates ADD KEY idx_templates_owner (owner_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND index_name = 'idx_templates_owner'
);
PREPARE stmt FROM @add_templates_owner_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_templates_owner_fk = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE templates ADD CONSTRAINT fk_templates_owner FOREIGN KEY (owner_id) REFERENCES users(id)',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND constraint_name = 'fk_templates_owner'
);
PREPARE stmt FROM @add_templates_owner_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
