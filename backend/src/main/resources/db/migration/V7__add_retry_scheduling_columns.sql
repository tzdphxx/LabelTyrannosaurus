ALTER TABLE ai_review_results
  ADD COLUMN next_retry_at DATETIME(3) NULL AFTER retry_count,
  ADD INDEX idx_ai_review_results_retry (status, next_retry_at);
