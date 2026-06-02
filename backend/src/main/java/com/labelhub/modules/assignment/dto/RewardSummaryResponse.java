package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "奖励摘要响应")
public record RewardSummaryResponse(
        @Schema(description = "每条通过奖励积分", example = "10.00")
        BigDecimal rewardPerApproval,
        @Schema(description = "每条驳回扣分", example = "5.00")
        BigDecimal penaltyPerRejection,
        @Schema(description = "额外奖励的通过数阈值", example = "50")
        Integer bonusThreshold,
        @Schema(description = "达标后额外奖励积分", example = "100.00")
        BigDecimal bonusPoints) {
}
