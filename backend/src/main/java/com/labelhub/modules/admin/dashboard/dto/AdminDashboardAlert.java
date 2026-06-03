package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端看板异常提醒")
public record AdminDashboardAlert(
        @Schema(description = "提醒类型，用于前端区分提醒来源和展示图标", example = "REVIEW_BACKLOG")
        AdminDashboardAlertType type,
        @Schema(description = "提醒级别，用于前端区分展示强度", example = "WARNING")
        AdminDashboardAlertLevel level,
        @Schema(description = "提醒标题", example = "审核积压")
        String title,
        @Schema(description = "提醒说明文案", example = "当前有 31 条提交待审核")
        String description,
        @Schema(description = "前端可跳转的目标路径", example = "/app/reviewer/queue")
        String targetPath
) {
}
