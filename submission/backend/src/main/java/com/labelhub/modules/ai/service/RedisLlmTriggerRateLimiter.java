package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.redis.RateLimitService;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import org.springframework.stereotype.Component;

@Component
public class RedisLlmTriggerRateLimiter implements LlmTriggerRateLimiter {

    private final RateLimitService rateLimitService;

    public RedisLlmTriggerRateLimiter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean acquire(Long taskId, Long userId, Long providerId) {
        String platformKey = RedisKeyBuilder.llmRate("platform", providerId);
        if (!rateLimitService.tryAcquire(platformKey, 1).allowed()) {
            return false;
        }
        String taskKey = RedisKeyBuilder.llmRate("task", taskId);
        if (!rateLimitService.tryAcquire(taskKey, 1).allowed()) {
            return false;
        }
        String userKey = RedisKeyBuilder.llmRate("user", userId);
        return rateLimitService.tryAcquire(userKey, 1).allowed();
    }
}
