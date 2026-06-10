package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.labelhub.infrastructure.llmtask.LlmTaskExecutionContext;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class LlmTriggerAsyncExecutorTest {

    @Mock private ThreadPoolTaskExecutor taskExecutor;
    @Mock private LlmTriggerService llmTriggerService;

    @Test
    void submitRunsTriggerWithTraceId() {
        LlmTriggerAsyncExecutor executor = new LlmTriggerAsyncExecutor(
                taskExecutor, providerFor(llmTriggerService));
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(invocation -> {
            assertThat(LlmTaskExecutionContext.currentTraceId()).isEqualTo("trace-trigger");
            return null;
        }).when(llmTriggerService).executeQueuedTrigger(100L);

        executor.submit(100L, "trace-trigger");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(llmTriggerService).executeQueuedTrigger(100L);
    }

    @Test
    void submitMarksFailedWhenExecutorRejectsTask() {
        LlmTriggerAsyncExecutor executor = new LlmTriggerAsyncExecutor(
                taskExecutor, providerFor(llmTriggerService));
        doThrow(new RejectedExecutionException("queue full"))
                .when(taskExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        executor.submit(100L, "trace-trigger");

        verify(llmTriggerService).failQueuedTrigger(100L,
                "LLM_TRIGGER_DISPATCH_REJECTED", "queue full");
    }

    @Test
    void submitMarksFailedWhenTriggerExecutionThrows() {
        LlmTriggerAsyncExecutor executor = new LlmTriggerAsyncExecutor(
                taskExecutor, providerFor(llmTriggerService));
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doThrow(new IllegalStateException("llm failed"))
                .when(llmTriggerService).executeQueuedTrigger(100L);

        executor.submit(100L, "trace-trigger");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(llmTriggerService).failQueuedTrigger(100L,
                "LLM_TRIGGER_EXECUTION_FAILED", "llm failed");
    }

    private static ObjectProvider<LlmTriggerService> providerFor(LlmTriggerService service) {
        return new ObjectProvider<>() {
            @Override
            public LlmTriggerService getObject(Object... args) {
                return service;
            }

            @Override
            public LlmTriggerService getIfAvailable() {
                return service;
            }

            @Override
            public LlmTriggerService getIfUnique() {
                return service;
            }

            @Override
            public LlmTriggerService getObject() {
                return service;
            }
        };
    }
}
