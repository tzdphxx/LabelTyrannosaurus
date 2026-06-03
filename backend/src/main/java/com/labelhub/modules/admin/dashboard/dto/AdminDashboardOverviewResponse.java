package com.labelhub.modules.admin.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminDashboardOverviewResponse(
        String range,
        AdminDashboardKpis kpis,
        AdminDashboardUserSummary userSummary,
        List<AdminDashboardTrendPoint> trend,
        Map<String, Long> taskStatusDistribution,
        List<AdminDashboardTopLabeler> topLabelers,
        List<AdminDashboardTopTask> topTasks,
        List<AdminDashboardAlert> alerts,
        LocalDateTime generatedAt
) {
}
