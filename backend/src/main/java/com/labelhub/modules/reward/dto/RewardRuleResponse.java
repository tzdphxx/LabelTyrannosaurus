package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "奖励规则响应")
public record RewardRuleResponse(
        @Schema(description = "规则记录 ID", example = "100")
        Long ruleId,
        @Schema(description = "所属任务 ID", example = "10")
        Long taskId,
        @Schema(description = "规则版本号，每次保存递增", example = "3")
        Integer effectiveVersion,
        @Schema(description = "奖励模式：APPROVED_ITEM（按通过条目计奖）", example = "APPROVED_ITEM")
        String rewardMode,
        @Schema(description = "单条奖励金额", example = "2.50")
        BigDecimal unitReward,
        @Schema(description = "奖励货币类型", example = "POINT")
        String rewardCurrency,
        @Schema(description = "奖励是否对标注员可见", example = "true")
        Boolean rewardVisible,
        @Schema(description = "规则生效时间")
        LocalDateTime effectiveAt,
        @Schema(description = "创建人用户 ID", example = "1")
        Long createdBy,
        @Schema(description = "规则创建时间")
        LocalDateTime createdAt) {
}
