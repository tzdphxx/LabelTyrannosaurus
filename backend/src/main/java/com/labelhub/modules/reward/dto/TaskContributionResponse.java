package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "任务贡献响应")
public record TaskContributionResponse(
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "任务标题") String taskTitle,
        @Schema(description = "提交数量") Integer submittedCount,
        @Schema(description = "通过数量") Integer approvedCount,
        @Schema(description = "驳回数量") Integer rejectedCount,
        @Schema(description = "总奖励金额") BigDecimal totalReward) {
}
