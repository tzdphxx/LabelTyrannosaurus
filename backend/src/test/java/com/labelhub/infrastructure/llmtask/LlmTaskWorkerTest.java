package com.labelhub.infrastructure.llmtask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.labelhub.infrastructure.redis.RedisLockService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmTaskWorkerTest {

    @Mock private LlmTaskQueueService queueService;
    @Mock private RedisLockService redisLockService;
    @Mock private LlmTaskHandler handler;

    private LlmTaskWorker worker;

    @BeforeEach
    void setUp() {
        when(handler.taskType()).thenReturn(LlmTaskType.PRE_ANNOTATION);
        worker = new LlmTaskWorker(queueService,
                new LlmTaskQueueProperties("group", 10, Duration.ofMillis(1), Duration.ofMinutes(5)),
                redisLockService,
                List.of(handler));
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(3);
            action.run();
            return null;
        }).when(redisLockService).withLock(any(), anyLong(), anyLong(), any(Runnable.class));
        lenient().when(queueService.read(any(), any(), anyInt(), any())).thenReturn(List.of());
        lenient().when(queueService.claimStale(any(), any(), any(), any(), anyInt()))
                .thenReturn(new LlmTaskClaimResult("0-0", List.of()));
    }

    @Test
    void pollAcknowledgesCompletedMessageWithoutHandlingIt() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.PRE_ANNOTATION), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(true);

        worker.poll();

        verify(handler, never()).handle(record.message());
        verify(queueService).ack(LlmTaskType.PRE_ANNOTATION, "1-0");
    }

    @Test
    void pollAcknowledgesMessageAfterSuccessfulHandling() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.PRE_ANNOTATION), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(false);

        worker.poll();

        verify(handler).handle(record.message());
        verify(queueService).ack(LlmTaskType.PRE_ANNOTATION, "1-0");
    }

    @Test
    void pollMarksFailureAndAcknowledgesMessageWhenHandlerThrows() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.PRE_ANNOTATION), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(false);
        IllegalStateException failure = new IllegalStateException("boom");
        doThrow(failure).when(handler).handle(record.message());

        worker.poll();

        verify(handler).onFailure(record.message(), failure);
        verify(queueService).ack(LlmTaskType.PRE_ANNOTATION, "1-0");
    }

    @Test
    void pollContinuesWhenOneTaskTypeReadFails() {
        when(queueService.read(eq(LlmTaskType.PRE_ANNOTATION), any(), eq(10), any()))
                .thenThrow(new IllegalStateException("redis timeout"));

        assertThatCode(() -> worker.poll()).doesNotThrowAnyException();

        verify(queueService, never()).read(eq(LlmTaskType.AI_REVIEW), any(), eq(10), any());
        verify(queueService, never()).read(eq(LlmTaskType.LLM_TRIGGER), any(), eq(10), any());
        verify(queueService).read(eq(LlmTaskType.PRE_ANNOTATION), any(), eq(10), any());
    }

    @Test
    void claimStaleContinuesWhenOneTaskTypeClaimFails() {
        when(queueService.claimStale(eq(LlmTaskType.PRE_ANNOTATION), any(), any(), any(), eq(10)))
                .thenThrow(new IllegalStateException("redis timeout"));

        assertThatCode(() -> worker.claimStale()).doesNotThrowAnyException();

        verify(queueService, times(1)).claimStale(any(), any(), any(), any(), eq(10));
        verify(queueService, never()).claimStale(eq(LlmTaskType.AI_REVIEW), any(), any(), any(), eq(10));
        verify(queueService, never()).claimStale(eq(LlmTaskType.LLM_TRIGGER), any(), any(), any(), eq(10));
    }

    private LlmTaskQueueRecord record() {
        return new LlmTaskQueueRecord("1-0", new LlmTaskQueueMessage(
                LlmTaskType.PRE_ANNOTATION,
                100L,
                10L,
                20L,
                100L,
                null,
                null,
                30L,
                "trace-worker",
                0,
                Instant.parse("2026-06-03T00:00:00Z")
        ));
    }
}
