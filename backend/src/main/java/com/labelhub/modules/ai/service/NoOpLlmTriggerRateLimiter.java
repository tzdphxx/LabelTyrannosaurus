package com.labelhub.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class NoOpLlmTriggerRateLimiter implements LlmTriggerRateLimiter {

    @Override
    public boolean acquire(Long taskId, Long userId, Long providerId) {
        return true;
    }
}
