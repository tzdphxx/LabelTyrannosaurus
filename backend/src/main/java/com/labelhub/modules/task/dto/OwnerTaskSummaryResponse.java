package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.Strategy;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "发布者任务摘要响应")
public record OwnerTaskSummaryResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "任务标题", example = "图像分类标注任务")
        String title,
        @Schema(description = "任务描述", example = "对商品图片进行类别标注")
        String description,
        @Schema(description = "任务状态", example = "PUBLISHED")
        TaskStatus status,
        @Schema(description = "任务标签列表", example = "[\"image\", \"classification\"]")
        List<String> tags,
        @Schema(description = "任务配额", example = "100")
        Integer quota,
        @Schema(description = "已被领取的数量", example = "45")
        Integer claimedCount,
        @Schema(description = "截止时间", example = "2026-06-30T23:59:59")
        LocalDateTime deadlineAt,
        @Schema(description = "分发策略", example = "FCFS")
        Strategy strategy,
        @Schema(description = "每条通过奖励积分", example = "10.00")
        BigDecimal rewardPerApproval,
        @Schema(description = "每条驳回扣分", example = "5.00")
        BigDecimal penaltyPerRejection,
        @Schema(description = "额外奖励的通过数阈值", example = "50")
        Integer bonusThreshold,
        @Schema(description = "达标后额外奖励积分", example = "100.00")
        BigDecimal bonusPoints,
        @Schema(description = "发布时间", example = "2026-05-01T10:00:00")
        LocalDateTime publishedAt,
        @Schema(description = "结束时间", example = "2026-06-30T23:59:59")
        LocalDateTime endedAt,
        @Schema(description = "创建时间", example = "2026-04-15T08:30:00")
        LocalDateTime createdAt,
        @Schema(description = "更新时间", example = "2026-05-01T10:00:00")
        LocalDateTime updatedAt
) {
}
