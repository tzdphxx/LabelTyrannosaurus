-- Promote task-bound templates to reusable owner template library.
-- This migration is idempotent because some long-lived development databases
-- already received the same schema changes under an older migration number.

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
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE templates MODIFY COLUMN owner_id BIGINT NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND column_name = 'owner_id'
      AND is_nullable = 'YES'
);
PREPARE stmt FROM @modify_templates_owner_id;
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

SET @add_templates_owner_index = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE templates ADD INDEX idx_templates_owner (owner_id, updated_at)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND index_name = 'idx_templates_owner'
);
PREPARE stmt FROM @add_templates_owner_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_templates_task_fk = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE templates DROP FOREIGN KEY fk_templates_task',
        'SELECT 1')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'templates'
      AND constraint_name = 'fk_templates_task'
);
PREPARE stmt FROM @drop_templates_task_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @modify_templates_task_id_nullable = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE templates MODIFY COLUMN task_id BIGINT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'templates'
      AND column_name = 'task_id'
      AND is_nullable = 'NO'
);
PREPARE stmt FROM @modify_templates_task_id_nullable;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_template_versions_owner_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE template_versions ADD COLUMN owner_id BIGINT NULL AFTER template_id',
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
JOIN templates t ON t.id = tv.template_id
SET tv.owner_id = t.owner_id
WHERE tv.owner_id IS NULL;

SET @modify_template_versions_owner_id = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE template_versions MODIFY COLUMN owner_id BIGINT NOT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND column_name = 'owner_id'
      AND is_nullable = 'YES'
);
PREPARE stmt FROM @modify_template_versions_owner_id;
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

SET @add_template_versions_owner_index = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE template_versions ADD INDEX idx_template_versions_owner (owner_id, created_at)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND index_name = 'idx_template_versions_owner'
);
PREPARE stmt FROM @add_template_versions_owner_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_template_versions_task_fk = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE template_versions DROP FOREIGN KEY fk_template_versions_task',
        'SELECT 1')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'template_versions'
      AND constraint_name = 'fk_template_versions_task'
);
PREPARE stmt FROM @drop_template_versions_task_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @modify_template_versions_task_id_nullable = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE template_versions MODIFY COLUMN task_id BIGINT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'template_versions'
      AND column_name = 'task_id'
      AND is_nullable = 'NO'
);
PREPARE stmt FROM @modify_template_versions_task_id_nullable;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
