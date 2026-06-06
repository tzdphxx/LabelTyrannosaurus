package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端看板任务排行榜条目")
public record AdminDashboardTopTask(
        @Schema(description = "任务 ID", example = "1001")
        Long taskId,
        @Schema(description = "任务标题", example = "商品质检任务")
        String title,
        @Schema(description = "周期内该任务有效提交数", example = "120")
        long submittedCount,
        @Schema(description = "周期内该任务审核通过数", example = "98")
        long approvedCount,
        @Schema(description = "周期内该任务审核打回数", example = "22")
        long rejectedCount
) {
}
