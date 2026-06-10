package com.labelhub.modules.role.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OwnerDashboardOverviewResponse(
        int trendDays,
        OwnerKpis kpis,
        Map<String, Long> taskStatusDistribution,
        List<DeliveryTrendPoint> deliveryTrend,
        QualitySummary qualitySummary,
        RewardSummary rewardSummary,
        List<AttentionTask> attentionTasks,
        List<RecentTask> recentTasks,
        LocalDateTime generatedAt
) {
    public record OwnerKpis(
            long totalTaskCount,
            long runningTaskCount,
            long claimedItemCount,
            long submittedItemCount,
            long pendingReviewCount,
            BigDecimal approvalRate,
            BigDecimal rewardCost
    ) {
    }

    public record DeliveryTrendPoint(
            LocalDate date,
            long claimedCount,
            long submittedCount,
            long approvedCount
    ) {
    }

    public record QualitySummary(
            long approvedCount,
            long rejectedCount,
            BigDecimal rejectionRate
    ) {
    }

    public record RewardSummary(
            BigDecimal totalRewardCost,
            long visibleTaskCount
    ) {
    }

    public record AttentionTask(
            Long taskId,
            String title,
            String type,
            DashboardAlertLevel level,
            String description,
            String targetPath
    ) {
    }

    public record RecentTask(
            Long taskId,
            String title,
            String status,
            BigDecimal progressRate,
            long pendingReviewCount,
            LocalDateTime updatedAt
    ) {
    }
}
