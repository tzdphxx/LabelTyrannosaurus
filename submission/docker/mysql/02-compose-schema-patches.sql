SET @add_export_jobs_trace_id = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE export_jobs ADD COLUMN trace_id varchar(128) DEFAULT NULL AFTER error_message',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'export_jobs'
    AND column_name = 'trace_id'
);

PREPARE add_export_jobs_trace_id_stmt FROM @add_export_jobs_trace_id;
EXECUTE add_export_jobs_trace_id_stmt;
DEALLOCATE PREPARE add_export_jobs_trace_id_stmt;
