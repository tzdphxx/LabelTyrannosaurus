package com.labelhub.modules.ai.service;

import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AiReviewRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiReviewRetryScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final AiReviewResultMapper aiReviewResultMapper;
    private volatile AiReviewRetryCallback retryCallback;

    public AiReviewRetryScheduler(ScheduledExecutorService aiReviewRetryExecutor,
                                  AiReviewResultMapper aiReviewResultMapper) {
        this.scheduler = aiReviewRetryExecutor;
        this.aiReviewResultMapper = aiReviewResultMapper;
    }

    public void setRetryCallback(AiReviewRetryCallback callback) {
        this.retryCallback = callback;
    }

    public void scheduleRetry(Long submissionId, Duration delay) {
        log.info("Scheduling AI review retry for submission {} in {}ms", submissionId, delay.toMillis());
        scheduler.schedule(() -> executeRetry(submissionId), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingRetries() {
        List<AiReviewResult> pending = aiReviewResultMapper.selectPendingRetries();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Recovering {} pending AI review retries", pending.size());
        for (AiReviewResult result : pending) {
            scheduler.schedule(() -> executeRetry(result.getSubmissionId()), 0, TimeUnit.MILLISECONDS);
        }
    }

    private void executeRetry(Long submissionId) {
        try {
            if (retryCallback != null) {
                retryCallback.onRetry(submissionId);
            }
        } catch (Exception e) {
            log.error("AI review retry failed for submission {}", submissionId, e);
        }
    }
}
