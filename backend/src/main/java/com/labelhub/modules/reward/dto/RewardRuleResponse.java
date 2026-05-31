package com.labelhub.modules.reward.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardRuleResponse(Long ruleId,
                                 Long taskId,
                                 Integer effectiveVersion,
                                 String rewardMode,
                                 BigDecimal unitReward,
                                 String rewardCurrency,
                                 Boolean rewardVisible,
                                 LocalDateTime effectiveAt,
                                 Long createdBy,
                                 LocalDateTime createdAt) {
}
