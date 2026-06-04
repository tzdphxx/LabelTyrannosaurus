package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "按任务聚合的贡献统计")
public record TaskContributionResponse(
        @Schema(description = "任务 ID", example = "10")
        Long taskId,
        @Schema(description = "任务标题", example = "图像分类标注")
        String taskTitle,
        @Schema(description = "该任务下已提交数")
        Integer submittedCount,
        @Schema(description = "该任务下已通过数")
        Integer approvedCount,
        @Schema(description = "该任务下已驳回数")
        Integer rejectedCount,
        @Schema(description = "该任务下累计获得奖励")
        BigDecimal totalReward) {
}
