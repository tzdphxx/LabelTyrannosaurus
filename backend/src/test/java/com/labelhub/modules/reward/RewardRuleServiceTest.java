package com.labelhub.modules.reward;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.reward.repository.RewardRuleRepositoryMapper;
import com.labelhub.modules.reward.service.RewardRuleService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardRuleServiceTest {

    private final TaskRepositoryMapper taskMapper = mock(TaskRepositoryMapper.class);
    private final RewardRuleRepositoryMapper rewardRuleMapper = mock(RewardRuleRepositoryMapper.class);
    private final CapturingRedisLockService redisLockService = new CapturingRedisLockService();
    private final RewardRuleService rewardRuleService = newService(taskMapper, rewardRuleMapper, redisLockService);

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void ownerSavesApprovedItemRuleAsNextVersion() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        when(rewardRuleMapper.selectMaxVersionByTaskId(1L)).thenReturn(2);
        when(rewardRuleMapper.insert(any(RewardRuleEntity.class))).thenAnswer(invocation -> {
            RewardRuleEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });

        var response = rewardRuleService.saveRule(1L,
                new RewardRuleRequest("APPROVED_ITEM", new BigDecimal("2.50"), "POINT", true));

        assertThat(response.ruleId()).isEqualTo(100L);
        assertThat(response.effectiveVersion()).isEqualTo(3);
        assertThat(response.effectiveAt()).isNotNull();
        assertThat(response.createdAt()).isNotNull();
        ArgumentCaptor<RewardRuleEntity> captor = ArgumentCaptor.forClass(RewardRuleEntity.class);
        verify(rewardRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getRewardMode()).isEqualTo("APPROVED_ITEM");
        assertThat(captor.getValue().getUnitReward()).isEqualByComparingTo("2.50");
        assertThat(captor.getValue().getEffectiveAt()).isEqualTo(response.effectiveAt());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(response.createdAt());
    }

    @Test
    void ownerSavesApprovedItemRuleWithinTaskScopedLock() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        when(rewardRuleMapper.selectMaxVersionByTaskId(1L)).thenReturn(2);
        when(rewardRuleMapper.insert(any(RewardRuleEntity.class))).thenAnswer(invocation -> {
            RewardRuleEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });

        RewardRuleResponse response = rewardRuleService.saveRule(1L,
                new RewardRuleRequest("APPROVED_ITEM", new BigDecimal("2.50"), "POINT", true));

        assertThat(response.ruleId()).isEqualTo(100L);
        assertThat(redisLockService.keys).containsExactly("lock:reward-rule:task:1");
        assertThat(redisLockService.waitMillis).containsExactly(1000L);
        assertThat(redisLockService.leaseMillis).containsExactly(5000L);
    }

    @Test
    void unsupportedRewardModeFailsBeforeInsert() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);

        assertThatThrownBy(() -> rewardRuleService.saveRule(1L,
                new RewardRuleRequest("TASK_BONUS", BigDecimal.ONE, "POINT", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    private void stubTask(Long ownerId) {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setOwnerId(ownerId);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private static RewardRuleService newService(TaskRepositoryMapper taskMapper,
                                                RewardRuleRepositoryMapper rewardRuleMapper,
                                                RedisLockService redisLockService) {
        for (Constructor<?> constructor : RewardRuleService.class.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 3) {
                try {
                    return (RewardRuleService) constructor.newInstance(taskMapper, rewardRuleMapper, redisLockService);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return new RewardRuleService(taskMapper, rewardRuleMapper);
    }

    private static final class CapturingRedisLockService implements RedisLockService {
        private final ArrayList<String> keys = new ArrayList<>();
        private final ArrayList<Long> waitMillis = new ArrayList<>();
        private final ArrayList<Long> leaseMillis = new ArrayList<>();

        @Override
        public boolean tryLock(String key, long waitMillis, long leaseMillis) {
            keys.add(key);
            this.waitMillis.add(waitMillis);
            this.leaseMillis.add(leaseMillis);
            return true;
        }

        @Override
        public void unlock(String key) {
        }

        @Override
        public <T> T withLock(String key, long waitMillis, long leaseMillis, Supplier<T> action) {
            keys.add(key);
            this.waitMillis.add(waitMillis);
            this.leaseMillis.add(leaseMillis);
            return action.get();
        }

        @Override
        public void withLock(String key, long waitMillis, long leaseMillis, Runnable action) {
            keys.add(key);
            this.waitMillis.add(waitMillis);
            this.leaseMillis.add(leaseMillis);
            action.run();
        }
    }
}
