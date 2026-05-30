package com.labelhub.modules.reward.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.reward.repository.RewardRuleRepositoryMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 奖励规则应用服务。BE-B 只按任务追加规则版本，不回写 BE-A 的任务发布状态。
 */
@Service
public class RewardRuleService {

    public static final String APPROVED_ITEM_MODE = "APPROVED_ITEM";
    private static final long RULE_LOCK_WAIT_MILLIS = 1000L;
    private static final long RULE_LOCK_LEASE_MILLIS = 5000L;

    private final TaskRepositoryMapper taskMapper;
    private final RewardRuleRepositoryMapper rewardRuleMapper;
    private final RedisLockService redisLockService;

    public RewardRuleService(TaskRepositoryMapper taskMapper, RewardRuleRepositoryMapper rewardRuleMapper) {
        this(taskMapper, rewardRuleMapper, new NoopRedisLockService());
    }

    @Autowired
    public RewardRuleService(TaskRepositoryMapper taskMapper,
                             RewardRuleRepositoryMapper rewardRuleMapper,
                             RedisLockService redisLockService) {
        this.taskMapper = taskMapper;
        this.rewardRuleMapper = rewardRuleMapper;
        this.redisLockService = redisLockService;
    }

    /**
     * 保存任务奖励规则的新版本。调用方必须是任务 Owner 或 ADMIN。
     */
    @Transactional
    public RewardRuleResponse saveRule(Long taskId, RewardRuleRequest request) {
        TaskEntity task = requireOwnedTask(taskId);
        validateRule(request);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        return redisLockService.withLock(
                RedisKeyBuilder.rewardRule(task.getId()),
                RULE_LOCK_WAIT_MILLIS,
                RULE_LOCK_LEASE_MILLIS,
                () -> saveRuleLocked(task, request, actor)
        );
    }

    private RewardRuleResponse saveRuleLocked(TaskEntity task, RewardRuleRequest request, CurrentUser actor) {
        int nextVersion = rewardRuleMapper.selectMaxVersionByTaskId(task.getId()) + 1;
        LocalDateTime now = LocalDateTime.now();

        RewardRuleEntity entity = new RewardRuleEntity();
        entity.setTaskId(task.getId());
        entity.setEffectiveVersion(nextVersion);
        entity.setRewardMode(APPROVED_ITEM_MODE);
        entity.setUnitReward(request.unitReward());
        entity.setRewardCurrency(defaultString(request.rewardCurrency(), "POINT"));
        entity.setRewardVisible(request.rewardVisible() == null || request.rewardVisible());
        entity.setEffectiveAt(now);
        entity.setCreatedBy(actor.userId());
        entity.setCreatedAt(now);
        rewardRuleMapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 查询任务最新奖励规则。调用方必须是任务 Owner 或 ADMIN。
     */
    public RewardRuleResponse getLatestRule(Long taskId) {
        requireOwnedTask(taskId);
        RewardRuleEntity rule = rewardRuleMapper.selectLatestByTaskId(taskId);
        if (rule == null) {
            throw new BusinessException(400102, "Reward rule not found");
        }
        return toResponse(rule);
    }

    private void validateRule(RewardRuleRequest request) {
        String mode = defaultString(request.rewardMode(), APPROVED_ITEM_MODE);
        if (!APPROVED_ITEM_MODE.equals(mode)) {
            throw new BusinessException(400102, "Unsupported reward mode");
        }
        if (request.unitReward() == null || request.unitReward().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400102, "Invalid unit reward");
        }
    }

    private TaskEntity requireOwnedTask(Long taskId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
        }
        return task;
    }

    private RewardRuleResponse toResponse(RewardRuleEntity rule) {
        return new RewardRuleResponse(
                rule.getId(),
                rule.getTaskId(),
                rule.getEffectiveVersion(),
                rule.getRewardMode(),
                rule.getUnitReward(),
                rule.getRewardCurrency(),
                rule.getRewardVisible(),
                rule.getEffectiveAt(),
                rule.getCreatedBy(),
                rule.getCreatedAt()
        );
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static final class NoopRedisLockService implements RedisLockService {

        @Override
        public boolean tryLock(String key, long waitMillis, long leaseMillis) {
            return true;
        }

        @Override
        public void unlock(String key) {
        }

        @Override
        public <T> T withLock(String key, long waitMillis, long leaseMillis, Supplier<T> action) {
            return action.get();
        }

        @Override
        public void withLock(String key, long waitMillis, long leaseMillis, Runnable action) {
            action.run();
        }
    }
}
