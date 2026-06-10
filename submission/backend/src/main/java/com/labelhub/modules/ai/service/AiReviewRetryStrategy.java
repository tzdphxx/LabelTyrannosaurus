package com.labelhub.modules.ai.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class AiReviewRetryStrategy {

    private static final long BASE_DELAY_MS = 2000L;
    private static final long MAX_DELAY_MS = 60000L;
    private static final long RATE_LIMITED_BASE_DELAY_MS = 10000L;
    private static final double JITTER_FACTOR = 0.4;
    private static final Set<String> NON_RETRYABLE_CODES = Set.of("INVALID_AI_REVIEW_OUTPUT");

    public Duration computeDelay(int retryCount, boolean rateLimited) {
        long base = rateLimited ? RATE_LIMITED_BASE_DELAY_MS : BASE_DELAY_MS;
        long raw = Math.min(base * (1L << retryCount), MAX_DELAY_MS);
        double jitter = 1.0 - JITTER_FACTOR / 2 + ThreadLocalRandom.current().nextDouble() * JITTER_FACTOR;
        return Duration.ofMillis(Math.round(raw * jitter));
    }

    public boolean isRetryable(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return !NON_RETRYABLE_CODES.contains(errorCode);
    }

    public boolean hasRetriesRemaining(int retryCount, int maxRetry) {
        return retryCount < maxRetry;
    }
}
