package com.labelhub.infrastructure.redis;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonRateLimitService implements RateLimitService {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;

    public RedissonRateLimitService(RedissonClient redissonClient, RateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public RateLimitResult tryAcquire(String key, long permits) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, properties.defaultRate(), properties.defaultInterval());
        boolean allowed = rateLimiter.tryAcquire(permits);
        if (allowed) {
            return new RateLimitResult(true, 0L);
        }
        return new RateLimitResult(false, properties.defaultInterval().toMillis());
    }
}
