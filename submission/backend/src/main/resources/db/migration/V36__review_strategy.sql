-- V36: AI review multi-strategy support
-- Adds review strategy, vote model list, and dimension-level reviewer mapping.

ALTER TABLE ai_review_configs
    ADD COLUMN review_strategy VARCHAR(32) NOT NULL DEFAULT 'LIGHTWEIGHT'
        COMMENT '审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE',
    ADD COLUMN vote_models_json JSON DEFAULT NULL
        COMMENT '投票模型列表, JSON array of {providerId, modelName}',
    ADD COLUMN vote_min_agreement INT DEFAULT 2
        COMMENT '最少一致票数, 默认2',
    ADD COLUMN dimension_reviewers_json JSON DEFAULT NULL
        COMMENT '深度模式 维度->模型列表映射, JSON object';
