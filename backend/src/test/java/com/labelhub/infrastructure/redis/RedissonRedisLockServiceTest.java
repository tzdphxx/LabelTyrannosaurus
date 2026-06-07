package com.labelhub.infrastructure.redis;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class RedissonRedisLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    private RedissonRedisLockService redisLockService;

    @BeforeEach
    void setUp() {
        redisLockService = new RedissonRedisLockService(redissonClient);
    }

    @Test
    void delegatesTryLockToRedissonLock() throws InterruptedException {
        when(redissonClient.getLock("lock:key")).thenReturn(lock);

        redisLockService.tryLock("lock:key", 2000, 10000);

        verify(lock).tryLock(2000, 10000, TimeUnit.MILLISECONDS);
    }

    @Test
    void unlocksOnlyWhenCurrentThreadHoldsLock() {
        when(redissonClient.getLock("lock:key")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        redisLockService.unlock("lock:key");

        verify(lock).unlock();
    }
}
