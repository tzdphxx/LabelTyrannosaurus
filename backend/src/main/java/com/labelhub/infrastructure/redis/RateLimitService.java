package com.labelhub.infrastructure.redis;

public interface RateLimitService {

    RateLimitResult tryAcquire(String key, long permits);
}
