package com.labelhub.infrastructure.redis;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedissonRateLimitService implements RateLimitService {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;
    private final Map<String, String> configHashes = new ConcurrentHashMap<>();

    public RedissonRateLimitService(RedissonClient redissonClient, RateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public RateLimitResult tryAcquire(String key, long permits) {
        return tryAcquire(key, permits, properties.defaultRate(), properties.defaultInterval());
    }

    @Override
    public RateLimitResult tryAcquire(String key, long permits, long rate, Duration interval) {
        long effectiveRate = rate > 0 ? rate : properties.defaultRate();
        Duration effectiveInterval = interval == null || interval.isNegative() || interval.isZero()
                ? properties.defaultInterval()
                : interval;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        String currentHash = computeConfigHash(effectiveRate, effectiveInterval);
        String previousHash = configHashes.put(key, currentHash);
        if (previousHash != null && !previousHash.equals(currentHash)) {
            rateLimiter.delete();
        }
        rateLimiter.trySetRate(
                RateType.OVERALL,
                effectiveRate,
                effectiveInterval,
                effectiveInterval.multipliedBy(2));
        boolean allowed = rateLimiter.tryAcquire(permits);
        if (allowed) {
            return new RateLimitResult(true, 0L);
        }
        return new RateLimitResult(false, effectiveInterval.toMillis());
    }

    private String computeConfigHash(long rate, Duration interval) {
        return rate + ":" + interval.toMillis();
    }
}
