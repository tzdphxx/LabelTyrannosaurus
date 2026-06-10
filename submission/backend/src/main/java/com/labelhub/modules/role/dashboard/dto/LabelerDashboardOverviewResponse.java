package com.labelhub.modules.role.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LabelerDashboardOverviewResponse(
        String range,
        LabelerKpis kpis,
        List<ContributionTrendPoint> contributionTrend,
        List<TaskContribution> taskContributions,
        TodoSummary todoSummary,
        List<Alert> alerts,
        LocalDateTime generatedAt
) {
    public record LabelerKpis(
            long claimedCount,
            long submittedCount,
            long approvedCount,
            long rejectedCount,
            BigDecimal approvalRate,
            BigDecimal periodReward,
            BigDecimal totalReward,
            long reworkCount
    ) {
    }

    public record ContributionTrendPoint(
            LocalDate date,
            long submittedCount,
            long approvedCount,
            BigDecimal reward
    ) {
    }

    public record TaskContribution(
            Long taskId,
            String taskTitle,
            long submittedCount,
            long approvedCount,
            BigDecimal totalReward,
            String targetPath
    ) {
    }

    public record TodoSummary(
            long claimedNotSubmittedCount,
            long rejectedNeedFixCount,
            long continuableTaskCount
    ) {
    }

    public record Alert(
            String type,
            DashboardAlertLevel level,
            String title,
            String description,
            String targetPath
    ) {
    }
}
