package com.labelhub.infrastructure.redis;

public record RateLimitResult(boolean allowed, long retryAfterMillis) {
}
