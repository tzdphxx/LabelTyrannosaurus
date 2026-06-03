package com.labelhub.infrastructure.llmtask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(handler.taskType()).thenReturn(LlmTaskType.AI_REVIEW);
        worker = new LlmTaskWorker(queueService,
                new LlmTaskQueueProperties("group", 10, Duration.ofMillis(1), Duration.ofMinutes(5)),
                redisLockService,
                List.of(handler));
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(3);
            action.run();
            return null;
        }).when(redisLockService).withLock(any(), anyLong(), anyLong(), any(Runnable.class));
    }

    @Test
    void pollAcknowledgesCompletedMessageWithoutHandlingIt() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.AI_REVIEW), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(true);

        worker.poll();

        verify(handler, never()).handle(record.message());
        verify(queueService).ack(LlmTaskType.AI_REVIEW, "1-0");
    }

    @Test
    void pollAcknowledgesMessageAfterSuccessfulHandling() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.AI_REVIEW), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(false);

        worker.poll();

        verify(handler).handle(record.message());
        verify(queueService).ack(LlmTaskType.AI_REVIEW, "1-0");
    }

    @Test
    void pollDoesNotAckMessageWhenHandlerThrows() {
        LlmTaskQueueRecord record = record();
        when(queueService.read(eq(LlmTaskType.AI_REVIEW), any(), eq(10), any())).thenReturn(List.of(record));
        when(handler.isCompleted(record.message())).thenReturn(false);
        doThrow(new IllegalStateException("boom")).when(handler).handle(record.message());

        worker.poll();

        verify(queueService, never()).ack(LlmTaskType.AI_REVIEW, "1-0");
    }

    private LlmTaskQueueRecord record() {
        return new LlmTaskQueueRecord("1-0", new LlmTaskQueueMessage(
                LlmTaskType.AI_REVIEW,
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
