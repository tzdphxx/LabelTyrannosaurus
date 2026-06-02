package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "奖励摘要响应")
public record RewardSummaryResponse(
        @Schema(description = "奖励模式") String rewardMode,
        @Schema(description = "单位奖励金额") BigDecimal unitReward,
        @Schema(description = "奖励货币") String rewardCurrency) {
}
