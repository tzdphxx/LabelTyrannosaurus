package com.labelhub.modules.ai.service;

public class NoOpLlmTriggerRateLimiter implements LlmTriggerRateLimiter {

    @Override
    public boolean acquire(Long taskId, Long userId, Long providerId) {
        return true;
    }
}
