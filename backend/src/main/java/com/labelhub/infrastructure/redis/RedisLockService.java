package com.labelhub.infrastructure.redis;

import java.util.function.Supplier;

public interface RedisLockService {

    boolean tryLock(String key, long waitMillis, long leaseMillis);

    void unlock(String key);

    <T> T withLock(String key, long waitMillis, long leaseMillis, Supplier<T> action);

    void withLock(String key, long waitMillis, long leaseMillis, Runnable action);
}
