package com.labelhub.modules.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentRunServiceTest {

    @Mock
    private AgentRunMapper agentRunMapper;

    private AgentRunService service;

    @BeforeEach
    void setUp() {
        service = new AgentRunService(agentRunMapper);
    }

    @Test
    void createsPendingAgentRun() {
        when(agentRunMapper.insert(any(AgentRun.class))).thenAnswer(inv -> {
            AgentRun run = inv.getArgument(0);
            run.setId(1L);
            return 1;
        });

        AgentRun run = service.create("AI_REVIEW", 10L, 1L, "gpt-4", "v1", "{\"q\":\"test\"}");

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.PENDING);
        assertThat(run.getAgentType()).isEqualTo("AI_REVIEW");
        assertThat(run.getSubmissionId()).isEqualTo(10L);
        assertThat(run.getProviderId()).isEqualTo(1L);
        assertThat(run.getModelName()).isEqualTo("gpt-4");
        assertThat(run.getPromptVersion()).isEqualTo("v1");
        assertThat(run.getInputSnapshot()).isEqualTo("{\"q\":\"test\"}");
        verify(agentRunMapper).insert(any(AgentRun.class));
    }

    @Test
    void createsRunWithTraceIdAndQueuedAt() {
        when(agentRunMapper.insert(any(AgentRun.class))).thenAnswer(inv -> {
            AgentRun run = inv.getArgument(0);
            run.setId(1L);
            return 1;
        });

        AgentRun run = service.create("AI_REVIEW", 10L, 1L, "qwen-plus", "v1",
                "{\"q\":\"test\"}", null, "trace-ai");

        assertThat(run.getTraceId()).isEqualTo("trace-ai");
        assertThat(run.getQueuedAt()).isNotNull();
        verify(agentRunMapper).insert(any(AgentRun.class));
    }

    @Test
    void eachCreateProducesNewRun() {
        when(agentRunMapper.insert(any(AgentRun.class))).thenReturn(1);

        AgentRun first = service.create("AI_REVIEW", 10L, null, null, null, null);
        AgentRun second = service.create("AI_REVIEW", 10L, null, null, null, null);

        assertThat(first).isNotSameAs(second);
        verify(agentRunMapper, times(2)).insert(any(AgentRun.class));
    }

    @Test
    void startSetsRunningAndStartedAt() {
        AgentRun run = pendingRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.startIfStatus(eq(1L), eq(AgentRunStatus.PENDING), eq(AgentRunStatus.RUNNING),
                any(LocalDateTime.class))).thenReturn(1);

        service.start(1L);

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.RUNNING);
        assertThat(run.getStartedAt()).isNotNull();
        verify(agentRunMapper).startIfStatus(eq(1L), eq(AgentRunStatus.PENDING), eq(AgentRunStatus.RUNNING),
                eq(run.getStartedAt()));
    }

    @Test
    void startThrowsBusinessExceptionWhenCasUpdateMisses() {
        AgentRun run = pendingRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.startIfStatus(eq(1L), eq(AgentRunStatus.PENDING), eq(AgentRunStatus.RUNNING),
                any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void completeSetsSuccessOutputAndFinishedAt() {
        AgentRun run = runningRun();
        run.setStartedAt(java.time.LocalDateTime.now().minusNanos(123_000_000));
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.completeIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.SUCCESS),
                eq("{\"score\":0.9}"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(1);

        service.complete(1L, "{\"score\":0.9}");

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(run.getOutputSnapshot()).isEqualTo("{\"score\":0.9}");
        assertThat(run.getFinishedAt()).isNotNull();
        assertThat(run.getLatencyMs()).isGreaterThanOrEqualTo(0L);
        verify(agentRunMapper).completeIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.SUCCESS),
                eq("{\"score\":0.9}"), eq(run.getFinishedAt()), eq(run.getLatencyMs()));
    }

    @Test
    void completeThrowsBusinessExceptionWhenCasUpdateMisses() {
        AgentRun run = runningRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.completeIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.SUCCESS),
                eq("{\"score\":0.9}"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(0);

        assertThatThrownBy(() -> service.complete(1L, "{\"score\":0.9}"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void failSetsFailedStatusAndError() {
        AgentRun run = runningRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.FAILED),
                eq("LLM timeout"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(1);

        service.fail(1L, AgentRunStatus.FAILED, "LLM timeout");

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("LLM timeout");
        assertThat(run.getFinishedAt()).isNotNull();
        verify(agentRunMapper).failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.FAILED),
                eq("LLM timeout"), eq(run.getFinishedAt()), eq(run.getLatencyMs()));
    }

    @Test
    void failThrowsBusinessExceptionWhenCasUpdateMisses() {
        AgentRun run = runningRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.FAILED),
                eq("LLM timeout"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(0);

        assertThatThrownBy(() -> service.fail(1L, AgentRunStatus.FAILED, "LLM timeout"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void failSetsRateLimitedStatus() {
        AgentRun run = runningRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.RATE_LIMITED),
                eq("quota exceeded"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(1);

        service.fail(1L, AgentRunStatus.RATE_LIMITED, "quota exceeded");

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.RATE_LIMITED);
        assertThat(run.getFinishedAt()).isNotNull();
        verify(agentRunMapper).failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.RATE_LIMITED),
                eq("quota exceeded"), eq(run.getFinishedAt()), eq(run.getLatencyMs()));
    }

    @Test
    void failSetsManualRequiredStatus() {
        AgentRun run = runningRun();
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        when(agentRunMapper.failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.MANUAL_REQUIRED),
                eq("max retries exceeded"), any(LocalDateTime.class), nullable(Long.class))).thenReturn(1);

        service.fail(1L, AgentRunStatus.MANUAL_REQUIRED, "max retries exceeded");

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.MANUAL_REQUIRED);
        assertThat(run.getFinishedAt()).isNotNull();
        verify(agentRunMapper).failIfStatus(eq(1L), eq(AgentRunStatus.RUNNING), eq(AgentRunStatus.MANUAL_REQUIRED),
                eq("max retries exceeded"), eq(run.getFinishedAt()), eq(run.getLatencyMs()));
    }

    @Test
    void failRejectsNonFailureStatus() {
        assertThatThrownBy(() -> service.fail(1L, AgentRunStatus.PENDING, "bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAILED, RATE_LIMITED or MANUAL_REQUIRED");
    }

    @Test
    void failRejectsSuccessStatus() {
        assertThatThrownBy(() -> service.fail(1L, AgentRunStatus.SUCCESS, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startThrowsWhenRunNotFound() {
        when(agentRunMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.start(99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findPendingReturnsRunOnlyWhenStatusIsPending() {
        AgentRun pending = pendingRun();
        when(agentRunMapper.selectById(1L)).thenReturn(pending);
        when(agentRunMapper.selectById(2L)).thenReturn(runningRun());

        assertThat(service.findPending(1L)).contains(pending);
        assertThat(service.findPending(2L)).isEmpty();
        assertThat(service.findPending(null)).isEmpty();
    }

    @Test
    void findActiveReturnsPendingOrRunningRunOnly() {
        AgentRun pending = pendingRun();
        AgentRun running = runningRun();
        AgentRun failed = pendingRun();
        failed.setStatus(AgentRunStatus.FAILED);
        when(agentRunMapper.selectById(1L)).thenReturn(pending);
        when(agentRunMapper.selectById(2L)).thenReturn(running);
        when(agentRunMapper.selectById(3L)).thenReturn(failed);

        assertThat(service.findActive(1L)).contains(pending);
        assertThat(service.findActive(2L)).contains(running);
        assertThat(service.findActive(3L)).isEmpty();
    }

    private AgentRun pendingRun() {
        AgentRun run = new AgentRun();
        run.setId(1L);
        run.setStatus(AgentRunStatus.PENDING);
        return run;
    }

    private AgentRun runningRun() {
        AgentRun run = pendingRun();
        run.setStatus(AgentRunStatus.RUNNING);
        return run;
    }
}
