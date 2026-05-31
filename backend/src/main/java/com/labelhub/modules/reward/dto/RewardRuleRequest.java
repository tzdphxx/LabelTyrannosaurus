package com.labelhub.modules.reward.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RewardRuleRequest(String rewardMode,
                                @NotNull @DecimalMin("0.00") BigDecimal unitReward,
                                String rewardCurrency,
                                Boolean rewardVisible) {
}
