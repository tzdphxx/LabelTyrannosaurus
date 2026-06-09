package com.labelhub.modules.role.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewerDashboardOverviewResponse(
        String range,
        QueueSummary queueSummary,
        ReviewerKpis kpis,
        List<ReviewTrendPoint> reviewTrend,
        AiReviewSummary aiReviewSummary,
        List<AttentionItem> attentionItems,
        List<RecentReviewed> recentReviewed,
        LocalDateTime generatedAt
) {
    public record QueueSummary(
            long pendingCount,
            long overduePendingCount,
            long manualRequiredCount,
            long conflictRequiredCount
    ) {
    }

    public record ReviewerKpis(
            long todayReviewedCount,
            long totalApproved,
            long totalRejected,
            BigDecimal approvalRate,
            long aiAttentionCount
    ) {
    }

    public record ReviewTrendPoint(
            LocalDate date,
            long reviewedCount,
            long approvedCount,
            long rejectedCount
    ) {
    }

    public record AiReviewSummary(
            long aiPassCount,
            long aiRejectCount,
            long manualReviewCount,
            long overriddenCount
    ) {
    }

    public record AttentionItem(
            Long reviewId,
            Long submissionId,
            Long taskId,
            String taskTitle,
            String type,
            DashboardAlertLevel level,
            String description,
            String targetPath
    ) {
    }

    public record RecentReviewed(
            Long reviewId,
            Long submissionId,
            String taskTitle,
            String labelerName,
            String result,
            LocalDateTime reviewedAt
    ) {
    }
}
