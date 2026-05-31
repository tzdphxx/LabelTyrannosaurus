package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.redis.RateLimitService;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import org.springframework.stereotype.Component;

@Component
public class RedisAiReviewRateLimiter implements AiReviewRateLimiter {

    private final RateLimitService rateLimitService;

    public RedisAiReviewRateLimiter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean acquire(Long taskId, Long providerId) {
        String platformKey = RedisKeyBuilder.llmRate("platform", providerId);
        if (!rateLimitService.tryAcquire(platformKey, 1).allowed()) {
            return false;
        }
        String taskKey = RedisKeyBuilder.llmRate("task", taskId);
        return rateLimitService.tryAcquire(taskKey, 1).allowed();
    }
}
