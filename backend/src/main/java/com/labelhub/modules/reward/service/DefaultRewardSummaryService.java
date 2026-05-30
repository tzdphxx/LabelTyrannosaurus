package com.labelhub.modules.reward.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.modules.assignment.dto.RewardSummaryResponse;
import com.labelhub.modules.reward.domain.RewardRule;
import com.labelhub.modules.reward.mapper.RewardRuleMapper;
import org.springframework.stereotype.Service;

@Service
public class DefaultRewardSummaryService implements RewardSummaryService {

    private final RewardRuleMapper rewardRuleMapper;

    public DefaultRewardSummaryService(RewardRuleMapper rewardRuleMapper) {
        this.rewardRuleMapper = rewardRuleMapper;
    }

    @Override
    public RewardSummaryResponse findRewardSummary(Long taskId, boolean rewardVisible) {
        if (!rewardVisible) {
            return null;
        }
        RewardRule rewardRule = rewardRuleMapper.selectOne(new QueryWrapper<RewardRule>()
                .eq("task_id", taskId)
                .orderByDesc("effective_version")
                .last("LIMIT 1"));
        if (rewardRule == null) {
            return null;
        }
        return new RewardSummaryResponse(
                rewardRule.getRewardMode(),
                rewardRule.getUnitReward(),
                rewardRule.getRewardCurrency()
        );
    }
}
