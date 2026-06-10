package com.labelhub.modules.ai.domain;

/**
 * AI 审核执行策略。
 * <ul>
 *   <li>{@link #LIGHTWEIGHT} — 单路 LLM，默认，兼容现有配置</li>
 *   <li>{@link #PARALLEL_VOTE} — 多模型并行投票</li>
 *   <li>{@link #DEEP_DIMENSION} — 每个评分维度专项模型，维度内多路投票</li>
 *   <li>{@link #AGENT_DEBATE} — 多 Agent 辩论模式</li>
 * </ul>
 */
public enum ReviewStrategy {

    LIGHTWEIGHT,
    PARALLEL_VOTE,
    DEEP_DIMENSION,
    AGENT_DEBATE
}
