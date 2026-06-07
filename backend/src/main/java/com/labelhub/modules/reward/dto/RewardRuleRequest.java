package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "保存奖励规则请求")
public record RewardRuleRequest(
        @Schema(description = "奖励模式，当前仅支持 APPROVED_ITEM（按通过条目计奖）",
                example = "APPROVED_ITEM")
        String rewardMode,
        @NotNull @DecimalMin("0.00")
        @Schema(description = "单条奖励金额", example = "2.50")
        BigDecimal unitReward,
        @Schema(description = "奖励货币类型，默认 POINT（平台积分）", example = "POINT")
        String rewardCurrency,
        @Schema(description = "奖励是否对标注员可见", example = "true")
        Boolean rewardVisible) {
}
