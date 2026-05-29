package com.labelhub.modules.reward;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
import com.labelhub.modules.reward.repository.RewardRuleMapper;
import com.labelhub.modules.reward.service.RewardRuleService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardRuleServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final RewardRuleMapper rewardRuleMapper = mock(RewardRuleMapper.class);
    private final RewardRuleService rewardRuleService = new RewardRuleService(taskMapper, rewardRuleMapper);

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
        ArgumentCaptor<RewardRuleEntity> captor = ArgumentCaptor.forClass(RewardRuleEntity.class);
        verify(rewardRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getRewardMode()).isEqualTo("APPROVED_ITEM");
        assertThat(captor.getValue().getUnitReward()).isEqualByComparingTo("2.50");
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
}
