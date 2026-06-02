package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AsyncAiReviewDispatcher implements AiReviewDispatcher {

    private final LlmTaskQueueService queueService;
    private final SubmissionMapper submissionMapper;
    private final AgentRunMapper agentRunMapper;
    private final TraceIdProvider traceIdProvider;

    public AsyncAiReviewDispatcher(LlmTaskQueueService queueService,
                                    SubmissionMapper submissionMapper,
                                    AgentRunMapper agentRunMapper,
                                    TraceIdProvider traceIdProvider) {
        this.queueService = queueService;
        this.submissionMapper = submissionMapper;
        this.agentRunMapper = agentRunMapper;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    public void enqueue(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        AgentRun agentRun = latestPendingRun(submissionId);
        queueService.enqueue(new LlmTaskQueueMessage(
                LlmTaskType.AI_REVIEW,
                submissionId,
                submission == null ? null : submission.getTaskId(),
                submission == null ? null : submission.getAssignmentId(),
                submissionId,
                null,
                null,
                agentRun == null ? null : agentRun.getId(),
                traceIdProvider.currentTraceId(),
                0,
                Instant.now()
        ));
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
