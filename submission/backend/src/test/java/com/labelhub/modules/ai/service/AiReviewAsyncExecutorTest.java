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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class AiReviewAsyncExecutorTest {

    @Mock private ThreadPoolTaskExecutor taskExecutor;
    @Mock private AiAutoReviewService aiAutoReviewService;

    @Test
    void submitRunsReviewWithTraceId() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(invocation -> {
            assertThat(LlmTaskExecutionContext.currentTraceId()).isEqualTo("trace-async");
            return null;
        }).when(aiAutoReviewService).executeQueuedReview(100L, 200L);

        executor.submit(100L, 200L, "trace-async");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(aiAutoReviewService).executeQueuedReview(100L, 200L);
    }

    @Test
    void submitMarksFailedWhenExecutorRejectsTask() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        doThrow(new RejectedExecutionException("queue full"))
                .when(taskExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        executor.submit(100L, 200L, "trace-async");

        verify(aiAutoReviewService).failQueuedReview(100L, 200L,
                "AI_REVIEW_DISPATCH_REJECTED", "queue full");
    }

    @Test
    void submitMarksFailedWhenReviewExecutionThrows() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doThrow(new IllegalStateException("llm failed"))
                .when(aiAutoReviewService).executeQueuedReview(100L, 200L);

        executor.submit(100L, 200L, "trace-async");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(aiAutoReviewService).failQueuedReview(100L, 200L,
                "AI_REVIEW_EXECUTION_FAILED", "llm failed");
    }

    @Test
    void submitRetryRunsReviewRetryWithTraceId() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(invocation -> {
            assertThat(LlmTaskExecutionContext.currentTraceId()).isEqualTo("trace-retry");
            return null;
        }).when(aiAutoReviewService).retryReview(100L);

        executor.submitRetry(100L, "trace-retry");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(aiAutoReviewService).retryReview(100L);
    }

    @Test
    void submitRetryMarksFailedWhenExecutorRejectsTask() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        doThrow(new RejectedExecutionException("retry queue full"))
                .when(taskExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        executor.submitRetry(100L, "trace-retry");

        verify(aiAutoReviewService).failRetryReview(100L,
                "AI_REVIEW_DISPATCH_REJECTED", "retry queue full");
    }

    @Test
    void submitRetryMarksFailedWhenReviewRetryThrows() {
        AiReviewAsyncExecutor executor = new AiReviewAsyncExecutor(taskExecutor, aiAutoReviewService);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doThrow(new IllegalStateException("retry failed"))
                .when(aiAutoReviewService).retryReview(100L);

        executor.submitRetry(100L, "trace-retry");

        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(aiAutoReviewService).failRetryReview(100L,
                "AI_REVIEW_EXECUTION_FAILED", "retry failed");
    }
}
