-- Add missing AI review failure fields used by AiReviewResult and retry persistence.
SET @add_ai_review_results_error_code = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_results ADD COLUMN error_code VARCHAR(100) NULL AFTER next_retry_at',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_results'
      AND column_name = 'error_code'
);
PREPARE add_ai_review_results_error_code_stmt FROM @add_ai_review_results_error_code;
EXECUTE add_ai_review_results_error_code_stmt;
DEALLOCATE PREPARE add_ai_review_results_error_code_stmt;

SET @add_ai_review_results_error_message = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_results ADD COLUMN error_message VARCHAR(500) NULL AFTER error_code',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_results'
      AND column_name = 'error_message'
);
PREPARE add_ai_review_results_error_message_stmt FROM @add_ai_review_results_error_message;
EXECUTE add_ai_review_results_error_message_stmt;
DEALLOCATE PREPARE add_ai_review_results_error_message_stmt;
