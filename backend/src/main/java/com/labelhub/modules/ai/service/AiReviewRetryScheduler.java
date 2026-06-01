package com.labelhub.modules.ai.service;

import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiReviewRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiReviewRetryScheduler.class);

    private final AiReviewResultMapper aiReviewResultMapper;
    private final SubmissionMapper submissionMapper;
    private final LlmTaskQueueService queueService;
    private final TraceIdProvider traceIdProvider;

    public AiReviewRetryScheduler(AiReviewResultMapper aiReviewResultMapper,
                                  SubmissionMapper submissionMapper,
                                  LlmTaskQueueService queueService,
                                  TraceIdProvider traceIdProvider) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.submissionMapper = submissionMapper;
        this.queueService = queueService;
        this.traceIdProvider = traceIdProvider;
    }

    public void scheduleRetry(Long submissionId, java.time.Duration delay) {
        log.info("AI review retry for submission {} will be picked up after {}ms", submissionId, delay.toMillis());
    }

    @Scheduled(fixedDelayString = "${labelhub.ai.retry-scan-delay-ms:5000}")
    public void enqueueDueRetries() {
        List<AiReviewResult> pending = aiReviewResultMapper.selectPendingRetries();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Enqueueing {} due AI review retries", pending.size());
        for (AiReviewResult result : pending) {
            enqueueRetry(result);
        }
    }

    private void enqueueRetry(AiReviewResult result) {
        Submission submission = submissionMapper.selectById(result.getSubmissionId());
        if (submission == null) {
            return;
        }
        queueService.enqueue(new LlmTaskQueueMessage(
                LlmTaskType.AI_REVIEW,
                result.getSubmissionId(),
                submission.getTaskId(),
                submission.getAssignmentId(),
                submission.getId(),
                null,
                null,
                result.getEffectiveRunId(),
                traceIdProvider.currentTraceId(),
                result.getRetryCount(),
                Instant.now()
        ));
    }
}
