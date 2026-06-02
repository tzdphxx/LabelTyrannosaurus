package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审核人任务摘要")
public record ReviewerTaskSummary(
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "任务标题") String taskTitle,
        @Schema(description = "待审核总数") int pendingCount,
        @Schema(description = "我的待审核数量") int myPendingCount,
        @Schema(description = "已审核总数") int totalReviewedCount
) {
}
