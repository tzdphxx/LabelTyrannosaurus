package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.dataset.dto.ItemSummaryResponse;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "任务市场视图")
public record TaskMarketResponse(
        @Schema(description = "任务摘要")
        TaskSummaryResponse task,
        @Schema(description = "当前可领取题目数", example = "55")
        Integer availableCount,
        @Schema(description = "当前用户已领取数", example = "3")
        Integer currentUserClaimedCount,
        @Schema(description = "奖励摘要")
        RewardSummaryResponse rewardSummary,
        @Schema(description = "任务描述")
        String description,
        @Schema(description = "富文本标注说明")
        String instructionRichText,
        @Schema(description = "可领取题目预览")
        List<ItemSummaryResponse> itemsPreview
) {}
