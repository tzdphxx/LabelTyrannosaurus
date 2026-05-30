package com.labelhub.modules.ai.service;

import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.async.AsyncJobCommand;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.async.AsyncJobType;
import org.springframework.stereotype.Component;

@Component
public class AsyncAiReviewDispatcher implements AiReviewDispatcher {

    private final AsyncJobService asyncJobService;
    private final AiAutoReviewService aiAutoReviewService;
    private final TraceIdProvider traceIdProvider;

    public AsyncAiReviewDispatcher(AsyncJobService asyncJobService,
                                    AiAutoReviewService aiAutoReviewService,
                                    TraceIdProvider traceIdProvider) {
        this.asyncJobService = asyncJobService;
        this.aiAutoReviewService = aiAutoReviewService;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    public void enqueue(Long submissionId) {
        asyncJobService.submit(new AsyncJobCommand(
                AsyncJobType.AI_REVIEW,
                submissionId,
                traceIdProvider.currentTraceId(),
                () -> aiAutoReviewService.reviewSubmission(submissionId)
        ));
    }
}
