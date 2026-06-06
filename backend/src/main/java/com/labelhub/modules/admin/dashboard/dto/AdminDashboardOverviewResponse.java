package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理端数据看板总览响应")
public record AdminDashboardOverviewResponse(
        @Schema(description = "统计周期编码，7d 表示近 7 天，30d 表示近 30 天", example = "7d")
        String range,
        @Schema(description = "平台运营核心 KPI 指标")
        AdminDashboardKpis kpis,
        @Schema(description = "用户总量、角色分布和新增/禁用用户摘要")
        AdminDashboardUserSummary userSummary,
        @Schema(description = "按自然日补齐的趋势数据，长度与 range 对应")
        List<AdminDashboardTrendPoint> trend,
        @Schema(description = "任务状态分布，固定包含 DRAFT、PUBLISHED、PAUSED、ENDED 四个 key")
        Map<String, Long> taskStatusDistribution,
        @Schema(description = "周期内提交量靠前的标注员列表")
        List<AdminDashboardTopLabeler> topLabelers,
        @Schema(description = "周期内提交量靠前的任务列表")
        List<AdminDashboardTopTask> topTasks,
        @Schema(description = "看板异常提醒列表，仅用于展示，不会自动处置业务状态")
        List<AdminDashboardAlert> alerts,
        @Schema(description = "本次看板数据生成时间", example = "2026-06-03T21:30:00")
        LocalDateTime generatedAt
) {
}
