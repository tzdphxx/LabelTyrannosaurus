package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llmtask.LlmTaskExecutionContext;
import com.labelhub.modules.ai.config.AiReviewExecutorConfig;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class AiReviewAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiReviewAsyncExecutor.class);
    private static final String DISPATCH_REJECTED = "AI_REVIEW_DISPATCH_REJECTED";
    private static final String EXECUTION_FAILED = "AI_REVIEW_EXECUTION_FAILED";

    private final ThreadPoolTaskExecutor executor;
    private final AiAutoReviewService aiAutoReviewService;

    public AiReviewAsyncExecutor(
            @Qualifier(AiReviewExecutorConfig.AI_REVIEW_TASK_EXECUTOR) ThreadPoolTaskExecutor executor,
            AiAutoReviewService aiAutoReviewService) {
        this.executor = executor;
        this.aiAutoReviewService = aiAutoReviewService;
    }

    public void submit(Long submissionId, Long agentRunId, String traceId) {
        try {
            executor.execute(() -> run(submissionId, agentRunId, traceId));
        } catch (RejectedExecutionException | IllegalStateException ex) {
            log.warn("AI review task rejected: submissionId={}, agentRunId={}",
                    submissionId, agentRunId, ex);
            aiAutoReviewService.failQueuedReview(submissionId, agentRunId,
                    DISPATCH_REJECTED, safeMessage(ex, "AI review executor rejected task"));
        }
    }

    public void submitRetry(Long submissionId, String traceId) {
        try {
            executor.execute(() -> runRetry(submissionId, traceId));
        } catch (RejectedExecutionException | IllegalStateException ex) {
            log.warn("AI review retry task rejected: submissionId={}", submissionId, ex);
            aiAutoReviewService.failRetryReview(submissionId,
                    DISPATCH_REJECTED, safeMessage(ex, "AI review executor rejected retry task"));
        }
    }

    private void run(Long submissionId, Long agentRunId, String traceId) {
        try {
            LlmTaskExecutionContext.runWithTraceId(traceId,
                    () -> aiAutoReviewService.executeQueuedReview(submissionId, agentRunId));
        } catch (Exception ex) {
            log.warn("AI review task execution failed: submissionId={}, agentRunId={}",
                    submissionId, agentRunId, ex);
            aiAutoReviewService.failQueuedReview(submissionId, agentRunId,
                    EXECUTION_FAILED, safeMessage(ex, "AI review execution failed"));
        }
    }

    private void runRetry(Long submissionId, String traceId) {
        try {
            LlmTaskExecutionContext.runWithTraceId(traceId,
                    () -> aiAutoReviewService.retryReview(submissionId));
        } catch (Exception ex) {
            log.warn("AI review retry task execution failed: submissionId={}", submissionId, ex);
            aiAutoReviewService.failRetryReview(submissionId,
                    EXECUTION_FAILED, safeMessage(ex, "AI review retry execution failed"));
        }
    }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
