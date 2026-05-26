package com.labelhub.infrastructure.redis;

public interface RedisLockService {

    boolean tryLock(String key, long waitMillis, long leaseMillis);

    void unlock(String key);
}
