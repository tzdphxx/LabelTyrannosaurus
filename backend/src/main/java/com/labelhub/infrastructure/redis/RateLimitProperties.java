package com.labelhub.infrastructure.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "labelhub.redis.rate-limit")
public record RateLimitProperties(long defaultRate, Duration defaultInterval) {

    public RateLimitProperties {
        if (defaultRate <= 0) {
            defaultRate = 60L;
        }
        if (defaultInterval == null || defaultInterval.isNegative() || defaultInterval.isZero()) {
            defaultInterval = Duration.ofMinutes(1);
        }
    }
}
