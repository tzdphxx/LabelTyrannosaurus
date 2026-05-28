package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiReviewRetryStrategyTest {

    private final AiReviewRetryStrategy strategy = new AiReviewRetryStrategy();

    @Test
    void computeDelayUsesExponentialBackoff() {
        Duration d0 = strategy.computeDelay(0, false);
        Duration d1 = strategy.computeDelay(1, false);
        Duration d2 = strategy.computeDelay(2, false);

        assertThat(d0.toMillis()).isBetween(1600L, 2400L);
        assertThat(d1.toMillis()).isBetween(3200L, 4800L);
        assertThat(d2.toMillis()).isBetween(6400L, 9600L);
    }

    @Test
    void computeDelayRespectsCap() {
        Duration d10 = strategy.computeDelay(10, false);
        assertThat(d10.toMillis()).isLessThanOrEqualTo(72000L);
    }

    @Test
    void computeDelayUsesLongerBaseForRateLimited() {
        Duration normal = strategy.computeDelay(0, false);
        Duration rateLimited = strategy.computeDelay(0, true);

        assertThat(rateLimited.toMillis()).isGreaterThan(normal.toMillis());
        assertThat(rateLimited.toMillis()).isBetween(8000L, 12000L);
    }

    @Test
    void isRetryableReturnsTrueForTransientErrors() {
        assertThat(strategy.isRetryable("TIMEOUT")).isTrue();
        assertThat(strategy.isRetryable("RATE_LIMITED")).isTrue();
        assertThat(strategy.isRetryable("GATEWAY_ERROR")).isTrue();
    }

    @Test
    void isRetryableReturnsFalseForInvalidOutput() {
        assertThat(strategy.isRetryable("INVALID_AI_REVIEW_OUTPUT")).isFalse();
    }

    @Test
    void isRetryableReturnsFalseForNull() {
        assertThat(strategy.isRetryable(null)).isFalse();
    }

    @Test
    void hasRetriesRemainingChecksCountAgainstMax() {
        assertThat(strategy.hasRetriesRemaining(0, 3)).isTrue();
        assertThat(strategy.hasRetriesRemaining(2, 3)).isTrue();
        assertThat(strategy.hasRetriesRemaining(3, 3)).isFalse();
        assertThat(strategy.hasRetriesRemaining(0, 0)).isFalse();
    }
}
