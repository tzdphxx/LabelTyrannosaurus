-- Add AI trace fields and keep LLM trigger component optional.
SET @relax_llm_trigger_component_id = (
    SELECT IF(COUNT(*) = 1,
        'ALTER TABLE llm_trigger_runs MODIFY COLUMN component_id VARCHAR(128) NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'llm_trigger_runs'
      AND column_name = 'component_id'
      AND is_nullable = 'NO'
);
PREPARE relax_llm_trigger_component_id_stmt FROM @relax_llm_trigger_component_id;
EXECUTE relax_llm_trigger_component_id_stmt;
DEALLOCATE PREPARE relax_llm_trigger_component_id_stmt;

SET @add_agent_runs_trace_id = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE agent_runs ADD COLUMN trace_id VARCHAR(128) NULL AFTER error_message',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_runs'
      AND column_name = 'trace_id'
);
PREPARE add_agent_runs_trace_id_stmt FROM @add_agent_runs_trace_id;
EXECUTE add_agent_runs_trace_id_stmt;
DEALLOCATE PREPARE add_agent_runs_trace_id_stmt;

SET @add_agent_runs_latency_ms = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE agent_runs ADD COLUMN latency_ms BIGINT NULL AFTER trace_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_runs'
      AND column_name = 'latency_ms'
);
PREPARE add_agent_runs_latency_ms_stmt FROM @add_agent_runs_latency_ms;
EXECUTE add_agent_runs_latency_ms_stmt;
DEALLOCATE PREPARE add_agent_runs_latency_ms_stmt;

SET @add_agent_runs_queued_at = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE agent_runs ADD COLUMN queued_at DATETIME(3) NULL AFTER latency_ms',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_runs'
      AND column_name = 'queued_at'
);
PREPARE add_agent_runs_queued_at_stmt FROM @add_agent_runs_queued_at;
EXECUTE add_agent_runs_queued_at_stmt;
DEALLOCATE PREPARE add_agent_runs_queued_at_stmt;
