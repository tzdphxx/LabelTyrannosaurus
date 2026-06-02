package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务统计响应")
public record TaskStatisticsResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "总数据条数", example = "200")
        int totalItems,
        @Schema(description = "已领取数量", example = "150")
        int claimedCount,
        @Schema(description = "已提交数量", example = "120")
        int submittedCount,
        @Schema(description = "已通过数量", example = "100")
        int approvedCount,
        @Schema(description = "已驳回数量", example = "20")
        int rejectedCount,
        @Schema(description = "待审核数量", example = "30")
        int pendingReviewCount,
        @Schema(description = "通过率", example = "83.3%")
        String passRate
) {
}
