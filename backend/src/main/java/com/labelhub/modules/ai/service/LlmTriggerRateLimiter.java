package com.labelhub.modules.ai.service;

public interface LlmTriggerRateLimiter {

    boolean acquire(Long taskId, Long userId, Long providerId);
}
