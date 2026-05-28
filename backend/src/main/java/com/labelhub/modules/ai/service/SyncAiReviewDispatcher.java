package com.labelhub.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class SyncAiReviewDispatcher implements AiReviewDispatcher {

    private final AiAutoReviewService aiAutoReviewService;

    public SyncAiReviewDispatcher(AiAutoReviewService aiAutoReviewService) {
        this.aiAutoReviewService = aiAutoReviewService;
    }

    @Override
    public void enqueue(Long submissionId) {
        aiAutoReviewService.reviewSubmission(submissionId);
    }
}
