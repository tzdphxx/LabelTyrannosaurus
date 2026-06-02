package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncAiReviewDispatcherTest {

    @Mock private LlmTaskQueueService queueService;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private AgentRunMapper agentRunMapper;
    @Mock private TraceIdProvider traceIdProvider;

    @Test
    void enqueueOnlyReusesPendingAiReviewRun() {
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                queueService, submissionMapper, agentRunMapper, traceIdProvider);
        Submission submission = new Submission();
        submission.setId(100L);
        submission.setTaskId(10L);
        submission.setAssignmentId(20L);
        when(submissionMapper.selectById(100L)).thenReturn(submission);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");

        dispatcher.enqueue(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AgentRun>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentRunMapper).selectOne(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains(AgentRunStatus.PENDING);

        ArgumentCaptor<LlmTaskQueueMessage> messageCaptor = ArgumentCaptor.forClass(LlmTaskQueueMessage.class);
        verify(queueService).enqueue(messageCaptor.capture());
        assertThat(messageCaptor.getValue().agentRunId()).isNull();
    }

    @Test
    void enqueueCarriesPendingRunIdWhenFound() {
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                queueService, submissionMapper, agentRunMapper, traceIdProvider);
        AgentRun pendingRun = new AgentRun();
        pendingRun.setId(300L);
        pendingRun.setStatus(AgentRunStatus.PENDING);
        when(agentRunMapper.selectOne(any())).thenReturn(pendingRun);

        dispatcher.enqueue(100L);

        ArgumentCaptor<LlmTaskQueueMessage> messageCaptor = ArgumentCaptor.forClass(LlmTaskQueueMessage.class);
        verify(queueService).enqueue(messageCaptor.capture());
        assertThat(messageCaptor.getValue().agentRunId()).isEqualTo(300L);
    }
}
