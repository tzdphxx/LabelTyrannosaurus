package com.labelhub.infrastructure.redis;

import java.time.Duration;

public interface RateLimitService {

    RateLimitResult tryAcquire(String key, long permits);

    RateLimitResult tryAcquire(String key, long permits, long rate, Duration interval);
}
