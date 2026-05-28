package com.labelhub.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RRateLimiter rateLimiter = mock(RRateLimiter.class);
    private final RateLimitProperties properties = new RateLimitProperties(5L, Duration.ofSeconds(60));
    private final RateLimitService rateLimitService = new RedissonRateLimitService(redissonClient, properties);

    @Test
    void allowsRequestWhenPermitsAreAvailable() {
        when(redissonClient.getRateLimiter("llm:rate:task:7")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(2L)).thenReturn(true);

        RateLimitResult result = rateLimitService.tryAcquire("llm:rate:task:7", 2L);

        assertThat(result.allowed()).isTrue();
        assertThat(result.retryAfterMillis()).isZero();
        verify(rateLimiter).trySetRate(RateType.OVERALL, 5L, Duration.ofSeconds(60));
    }

    @Test
    void returnsRetryAfterWhenPermitsAreExhausted() {
        when(redissonClient.getRateLimiter("llm:rate:task:7")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(6L)).thenReturn(false);

        RateLimitResult result = rateLimitService.tryAcquire("llm:rate:task:7", 6L);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterMillis()).isEqualTo(60_000L);
    }
}
