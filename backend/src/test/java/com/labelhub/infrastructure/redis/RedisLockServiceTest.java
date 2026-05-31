package com.labelhub.infrastructure.redis;

import com.labelhub.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLockServiceTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final RedisLockService redisLockService = new RedissonRedisLockService(redissonClient);

    @Test
    void buildsDocumentedRedisKeys() {
        assertThat(RedisKeyBuilder.claimLock(7L, 9L))
                .isEqualTo("lock:claim:task:7:item:9");
        assertThat(RedisKeyBuilder.assignmentDraft(11L))
                .isEqualTo("draft:assignment:11");
        assertThat(RedisKeyBuilder.llmRate("task", 13L))
                .isEqualTo("llm:rate:task:13");
        assertThat(RedisKeyBuilder.eventDedup("SubmissionApproved", "evt-15"))
                .isEqualTo("event:dedup:SubmissionApproved:evt-15");
        assertThat(RedisKeyBuilder.rewardRule(17L))
                .isEqualTo("lock:reward-rule:task:17");
    }

    @Test
    void withLockReturnsActionResultAndUnlocksHeldLock() throws Exception {
        when(redissonClient.getLock("lock:claim:task:7:item:9")).thenReturn(lock);
        when(lock.tryLock(100L, 1000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        String result = redisLockService.withLock(
                "lock:claim:task:7:item:9",
                100L,
                1000L,
                () -> "reserved"
        );

        assertThat(result).isEqualTo("reserved");
        verify(lock).unlock();
    }

    @Test
    void withLockUnlocksWhenActionThrows() throws Exception {
        when(redissonClient.getLock("lock:claim:task:7:item:9")).thenReturn(lock);
        when(lock.tryLock(100L, 1000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> redisLockService.withLock(
                "lock:claim:task:7:item:9",
                100L,
                1000L,
                () -> {
                    throw new IllegalStateException("boom");
                }
        )).isInstanceOf(IllegalStateException.class);

        verify(lock).unlock();
    }

    @Test
    void withLockRejectsWhenLockCannotBeAcquired() throws Exception {
        when(redissonClient.getLock("lock:claim:task:7:item:9")).thenReturn(lock);
        when(lock.tryLock(10L, 1000L, TimeUnit.MILLISECONDS)).thenReturn(false);

        assertThatThrownBy(() -> redisLockService.withLock(
                "lock:claim:task:7:item:9",
                10L,
                1000L,
                () -> "reserved"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409101);
        verify(lock, never()).unlock();
    }
}
