package com.labelhub.infrastructure.redis;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonRateLimitService implements RateLimitService {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;
    private volatile String configHash;

    public RedissonRateLimitService(RedissonClient redissonClient, RateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.configHash = computeConfigHash();
    }

    @Override
    public RateLimitResult tryAcquire(String key, long permits) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        String currentHash = computeConfigHash();
        if (!currentHash.equals(configHash)) {
            rateLimiter.delete();
            configHash = currentHash;
        }
        rateLimiter.trySetRate(
                RateType.OVERALL,
                properties.defaultRate(),
                properties.defaultInterval(),
                properties.defaultInterval().multipliedBy(2));
        boolean allowed = rateLimiter.tryAcquire(permits);
        if (allowed) {
            return new RateLimitResult(true, 0L);
        }
        return new RateLimitResult(false, properties.defaultInterval().toMillis());
    }

    private String computeConfigHash() {
        return properties.defaultRate() + ":" + properties.defaultInterval().toMillis();
    }
}
