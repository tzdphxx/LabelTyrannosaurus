-- Normalize AI review decision wording from RETURN to REJECT.
UPDATE ai_review_results
SET decision = 'REJECT'
WHERE decision = 'RETURN';

SET @drop_ai_review_decision_check = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE ai_review_results DROP CHECK chk_ai_review_results_decision')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'ai_review_results'
      AND constraint_name = 'chk_ai_review_results_decision'
);
PREPARE drop_ai_review_decision_check_stmt FROM @drop_ai_review_decision_check;
EXECUTE drop_ai_review_decision_check_stmt;
DEALLOCATE PREPARE drop_ai_review_decision_check_stmt;

ALTER TABLE ai_review_results
    ADD CONSTRAINT chk_ai_review_results_decision
        CHECK (decision IS NULL OR decision IN ('PASS', 'REJECT', 'MANUAL_REVIEW'));
