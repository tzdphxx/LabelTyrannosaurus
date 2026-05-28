package com.labelhub.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class NoOpAiReviewRateLimiter implements AiReviewRateLimiter {

    @Override
    public boolean acquire(Long taskId, Long providerId) {
        return true;
    }
}
