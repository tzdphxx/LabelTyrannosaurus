package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import org.springframework.stereotype.Component;

@Component
public class AsyncAiReviewDispatcher implements AiReviewDispatcher {

    private final AgentRunMapper agentRunMapper;
    private final TraceIdProvider traceIdProvider;
    private final AiReviewAsyncExecutor asyncExecutor;

    public AsyncAiReviewDispatcher(AgentRunMapper agentRunMapper,
                                    TraceIdProvider traceIdProvider,
                                    AiReviewAsyncExecutor asyncExecutor) {
        this.agentRunMapper = agentRunMapper;
        this.traceIdProvider = traceIdProvider;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void enqueue(Long submissionId) {
        AgentRun agentRun = latestPendingRun(submissionId);
        Long agentRunId = agentRun != null && agentRun.getStatus() == AgentRunStatus.PENDING
                ? agentRun.getId()
                : null;
        asyncExecutor.submit(submissionId, agentRunId, traceIdProvider.currentTraceId());
        /*
         * Redis Stream implementation retained for rollback/reference:
         *
         * Submission submission = submissionMapper.selectById(submissionId);
         * queueService.enqueue(new LlmTaskQueueMessage(
         *         LlmTaskType.AI_REVIEW,
         *         submissionId,
         *         submission == null ? null : submission.getTaskId(),
         *         submission == null ? null : submission.getAssignmentId(),
         *         submissionId,
         *         null,
         *         null,
         *         agentRunId,
         *         traceIdProvider.currentTraceId(),
         *         0,
         *         Instant.now()
         * ));
         */
    }

    private AgentRun latestPendingRun(Long submissionId) {
        return agentRunMapper.selectOne(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getSubmissionId, submissionId)
                .eq(AgentRun::getAgentType, "AI_REVIEW")
                .eq(AgentRun::getStatus, AgentRunStatus.PENDING)
                .orderByDesc(AgentRun::getId)
                .last("LIMIT 1"));
    }
}
