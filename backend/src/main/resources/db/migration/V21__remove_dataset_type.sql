-- Remove the deprecated dataset type model from dataset items and payloads.

UPDATE dataset_items
SET item_json = JSON_REMOVE(item_json, '$.datasetType')
WHERE JSON_CONTAINS_PATH(item_json, 'one', '$.datasetType');

SET @drop_dataset_items_type_index = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE dataset_items DROP INDEX idx_dataset_items_type',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'dataset_items'
      AND index_name = 'idx_dataset_items_type'
);
PREPARE drop_dataset_items_type_index_stmt FROM @drop_dataset_items_type_index;
EXECUTE drop_dataset_items_type_index_stmt;
DEALLOCATE PREPARE drop_dataset_items_type_index_stmt;

SET @drop_dataset_items_type_check = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE dataset_items DROP CHECK chk_dataset_items_type',
              'SELECT 1')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'dataset_items'
      AND constraint_name = 'chk_dataset_items_type'
      AND constraint_type = 'CHECK'
);
PREPARE drop_dataset_items_type_check_stmt FROM @drop_dataset_items_type_check;
EXECUTE drop_dataset_items_type_check_stmt;
DEALLOCATE PREPARE drop_dataset_items_type_check_stmt;

SET @drop_dataset_items_type_column = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE dataset_items DROP COLUMN dataset_type',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dataset_items'
      AND column_name = 'dataset_type'
);
PREPARE drop_dataset_items_type_column_stmt FROM @drop_dataset_items_type_column;
EXECUTE drop_dataset_items_type_column_stmt;
DEALLOCATE PREPARE drop_dataset_items_type_column_stmt;
