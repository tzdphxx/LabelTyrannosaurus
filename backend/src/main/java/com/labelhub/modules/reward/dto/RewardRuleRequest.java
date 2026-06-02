package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "奖励规则请求")
public record RewardRuleRequest(
        @Schema(description = "奖励模式") String rewardMode,
        @NotNull @DecimalMin("0.00") @Schema(description = "单位奖励金额") BigDecimal unitReward,
        @Schema(description = "奖励货币") String rewardCurrency,
        @Schema(description = "奖励是否可见") Boolean rewardVisible) {
}
