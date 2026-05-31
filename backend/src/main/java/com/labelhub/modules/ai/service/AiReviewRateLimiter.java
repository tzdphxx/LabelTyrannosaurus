package com.labelhub.modules.ai.service;

public interface AiReviewRateLimiter {

    boolean acquire(Long taskId, Long providerId);
}
