package com.labelhub.modules.reward.dto;

import com.labelhub.modules.reward.domain.RewardDirection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "奖励流水记录")
public record RewardLedgerResponse(
        @Schema(description = "流水 ID", example = "500")
        Long ledgerId,
        @Schema(description = "所属任务 ID", example = "10")
        Long taskId,
        @Schema(description = "关联提交 ID", example = "200")
        Long submissionId,
        @Schema(description = "关联分配 ID", example = "150")
        Long assignmentId,
        @Schema(description = "奖励金额", example = "2.50")
        BigDecimal amount,
        @Schema(description = "资金方向：CREDIT（正向奖励）/ DEBIT（冲正扣除）")
        RewardDirection direction,
        @Schema(description = "操作原因或备注")
        String reason,
        @Schema(description = "来源事件 ID，用于幂等去重", example = "evt-abc123")
        String sourceEventId,
        @Schema(description = "奖励类型：SUBMISSION_APPROVED / REWARD_REVERSED 等")
        String rewardType,
        @Schema(description = "流水创建时间")
        LocalDateTime createdAt) {
}
