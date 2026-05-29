package com.labelhub.modules.reward.service;

import com.labelhub.modules.assignment.dto.RewardSummaryResponse;

public interface RewardSummaryService {

    RewardSummaryResponse findRewardSummary(Long taskId, boolean rewardVisible);
}
