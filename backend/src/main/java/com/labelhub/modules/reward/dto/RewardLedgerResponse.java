package com.labelhub.modules.reward.dto;

import com.labelhub.modules.reward.domain.RewardDirection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "奖励台账响应")
public record RewardLedgerResponse(
        @Schema(description = "台账ID") Long ledgerId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "金额") BigDecimal amount,
        @Schema(description = "奖励方向") RewardDirection direction,
        @Schema(description = "原因") String reason,
        @Schema(description = "来源事件ID") String sourceEventId,
        @Schema(description = "奖励类型") String rewardType,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
