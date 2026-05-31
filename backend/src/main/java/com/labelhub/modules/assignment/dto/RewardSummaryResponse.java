package com.labelhub.modules.assignment.dto;

import java.math.BigDecimal;

public record RewardSummaryResponse(String rewardMode, BigDecimal unitReward, String rewardCurrency) {
}
