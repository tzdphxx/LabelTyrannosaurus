package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llmtask.LlmTaskExecutionContext;
import com.labelhub.modules.ai.config.LlmTriggerExecutorConfig;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class LlmTriggerAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(LlmTriggerAsyncExecutor.class);
    private static final String DISPATCH_REJECTED = "LLM_TRIGGER_DISPATCH_REJECTED";
    private static final String EXECUTION_FAILED = "LLM_TRIGGER_EXECUTION_FAILED";

    private final ThreadPoolTaskExecutor executor;
    private final ObjectProvider<LlmTriggerService> llmTriggerServiceProvider;

    public LlmTriggerAsyncExecutor(
            @Qualifier(LlmTriggerExecutorConfig.LLM_TRIGGER_TASK_EXECUTOR) ThreadPoolTaskExecutor executor,
            ObjectProvider<LlmTriggerService> llmTriggerServiceProvider) {
        this.executor = executor;
        this.llmTriggerServiceProvider = llmTriggerServiceProvider;
    }

    public void submit(Long triggerRunId, String traceId) {
        try {
            executor.execute(() -> run(triggerRunId, traceId));
        } catch (RejectedExecutionException | IllegalStateException ex) {
            log.warn("LLM trigger task rejected: triggerRunId={}", triggerRunId, ex);
            llmTriggerServiceProvider.getObject().failQueuedTrigger(triggerRunId,
                    DISPATCH_REJECTED, safeMessage(ex, "LLM trigger executor rejected task"));
        }
    }

    private void run(Long triggerRunId, String traceId) {
        try {
            LlmTaskExecutionContext.runWithTraceId(traceId,
                    () -> llmTriggerServiceProvider.getObject().executeQueuedTrigger(triggerRunId));
        } catch (Exception ex) {
            log.warn("LLM trigger task execution failed: triggerRunId={}", triggerRunId, ex);
            llmTriggerServiceProvider.getObject().failQueuedTrigger(triggerRunId,
                    EXECUTION_FAILED, safeMessage(ex, "LLM trigger execution failed"));
        }
    }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
