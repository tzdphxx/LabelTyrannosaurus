package com.labelhub.modules.ai.service;

@FunctionalInterface
public interface AiReviewRetryCallback {
    void onRetry(Long submissionId);
}
