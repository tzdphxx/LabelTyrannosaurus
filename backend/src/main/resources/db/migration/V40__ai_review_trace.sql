ALTER TABLE ai_review_results
    ADD COLUMN review_trace JSON NULL COMMENT 'AI review strategy execution trace' AFTER raw_response;
