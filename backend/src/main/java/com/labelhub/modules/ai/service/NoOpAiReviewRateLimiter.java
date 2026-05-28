package com.labelhub.modules.ai.service;

public class NoOpAiReviewRateLimiter implements AiReviewRateLimiter {

    @Override
    public boolean acquire(Long taskId, Long providerId) {
        return true;
    }
}
