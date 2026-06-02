package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "奖励规则响应")
public record RewardRuleResponse(
        @Schema(description = "规则ID") Long ruleId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "生效版本号") Integer effectiveVersion,
        @Schema(description = "奖励模式") String rewardMode,
        @Schema(description = "单位奖励金额") BigDecimal unitReward,
        @Schema(description = "奖励货币") String rewardCurrency,
        @Schema(description = "奖励是否可见") Boolean rewardVisible,
        @Schema(description = "生效时间") LocalDateTime effectiveAt,
        @Schema(description = "创建人ID") Long createdBy,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
