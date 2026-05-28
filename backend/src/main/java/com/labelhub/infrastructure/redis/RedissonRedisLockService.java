package com.labelhub.infrastructure.redis;

import com.labelhub.common.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedissonRedisLockService implements RedisLockService {

    private final RedissonClient redissonClient;

    public RedissonRedisLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, long waitMillis, long leaseMillis) {
        try {
            return redissonClient.getLock(key).tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <T> T withLock(String key, long waitMillis, long leaseMillis, Supplier<T> action) {
        if (!tryLock(key, waitMillis, leaseMillis)) {
            throw new BusinessException(409101, "Redis lock acquire timeout");
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }

    @Override
    public void withLock(String key, long waitMillis, long leaseMillis, Runnable action) {
        withLock(key, waitMillis, leaseMillis, () -> {
            action.run();
            return null;
        });
    }
}
