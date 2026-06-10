package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.web.RequestTraceIdProvider;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AsyncAiReviewDispatcherTest {

    @Mock private AgentRunMapper agentRunMapper;
    @Mock private TraceIdProvider traceIdProvider;
    @Mock private AiReviewAsyncExecutor asyncExecutor;

    @Test
    void enqueueOnlyReusesPendingAiReviewRun() {
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                agentRunMapper, traceIdProvider, asyncExecutor);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");

        dispatcher.enqueue(100L);

        // Verify that selectOne is called to look for an existing PENDING AI_REVIEW run.
        // The default mock returns null, so the dispatcher should enqueue with null agentRunId.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AgentRun>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentRunMapper).selectOne(wrapperCaptor.capture());

        verify(asyncExecutor).submit(100L, null, "trace-1");
    }

    @Test
    void enqueueCarriesPendingRunIdWhenFound() {
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                agentRunMapper, traceIdProvider, asyncExecutor);
        AgentRun pendingRun = new AgentRun();
        pendingRun.setId(300L);
        pendingRun.setStatus(AgentRunStatus.PENDING);
        when(agentRunMapper.selectOne(any())).thenReturn(pendingRun);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-2");

        dispatcher.enqueue(100L);

        verify(asyncExecutor).submit(100L, 300L, "trace-2");
    }

    @Test
    void enqueueDoesNotCarryNonPendingRunIdWhenMapperReturnsStaleRun() {
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                agentRunMapper, traceIdProvider, asyncExecutor);
        AgentRun failedRun = new AgentRun();
        failedRun.setId(301L);
        failedRun.setStatus(AgentRunStatus.FAILED);
        when(agentRunMapper.selectOne(any())).thenReturn(failedRun);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-3");

        dispatcher.enqueue(100L);

        verify(asyncExecutor).submit(100L, null, "trace-3");
    }

    @Test
    void enqueueGeneratesTraceIdWhenNoRequestContextIsBound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id"))
                .thenThrow(new IllegalStateException("No thread-bound request found"));
        RequestTraceIdProvider requestTraceIdProvider = new RequestTraceIdProvider(providerFor(request));
        AsyncAiReviewDispatcher dispatcher = new AsyncAiReviewDispatcher(
                agentRunMapper, requestTraceIdProvider, asyncExecutor);

        dispatcher.enqueue(100L);

        ArgumentCaptor<String> traceCaptor = ArgumentCaptor.forClass(String.class);
        verify(asyncExecutor).submit(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.isNull(), traceCaptor.capture());
        assertThat(traceCaptor.getValue()).isNotBlank();
    }

    private static ObjectProvider<HttpServletRequest> providerFor(HttpServletRequest request) {
        return new ObjectProvider<>() {
            @Override
            public HttpServletRequest getObject(Object... args) {
                return request;
            }

            @Override
            public HttpServletRequest getIfAvailable() {
                return request;
            }

            @Override
            public HttpServletRequest getIfUnique() {
                return request;
            }

            @Override
            public HttpServletRequest getObject() {
                return request;
            }
        };
    }
}
