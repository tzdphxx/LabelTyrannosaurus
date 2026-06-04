package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审核员工作台任务摘要")
public record ReviewerTaskSummary(
        @Schema(description = "任务 ID", example = "10")
        Long taskId,
        @Schema(description = "任务标题")
        String taskTitle,
        @Schema(description = "该任务下待审提交总数")
        int pendingCount,
        @Schema(description = "其中归属当前审核员的待审数")
        int myPendingCount,
        @Schema(description = "当前审核员在该任务下累计已审核数")
        int totalReviewedCount,
        @Schema(description = "该任务一级审核是否已被某审核员领取")
        boolean claimed,
        @Schema(description = "该任务一级审核是否由当前审核员领取")
        boolean claimedByMe
) {
}
