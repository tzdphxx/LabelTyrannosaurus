package com.labelhub.modules.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "任务摘要")
public record TaskSummaryResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "任务标题", example = "图像分类标注任务")
        String title,
        @Schema(description = "任务状态", example = "PUBLISHED")
        TaskStatus status,
        @Schema(description = "任务标签", example = "[\"image\", \"classification\"]")
        List<String> tags,
        @Schema(description = "任务配额", example = "100")
        Integer quota,
        @Schema(description = "已领取数", example = "45")
        Integer claimedCount,
        @Schema(description = "每条数据需要的标注份数", example = "1")
        Integer overlapCount,
        @Schema(description = "领取策略", example = "FCFS")
        ClaimStrategy strategy,
        @JsonProperty("max_claims_per_labeler")
        @Schema(description = "单人并发未完成上限（仅 QUOTA_GRAB 有效）", example = "10")
        Integer maxClaimsPerLabeler,
        @Schema(description = "截止时间", example = "2026-06-30T23:59:59")
        LocalDateTime deadlineAt,
        @Schema(description = "发布时间")
        LocalDateTime publishedAt,
        @Schema(description = "结束时间")
        LocalDateTime endedAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt,
        @Schema(description = "Reward rule")
        RewardRuleResponse rewardRule
) {}
