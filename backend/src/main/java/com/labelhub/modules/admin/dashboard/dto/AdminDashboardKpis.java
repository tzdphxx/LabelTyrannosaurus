package com.labelhub.modules.admin.dashboard.dto;

import java.math.BigDecimal;

public record AdminDashboardKpis(
        long activeTaskCount,
        long claimedCount,
        long submittedCount,
        long pendingReviewCount,
        BigDecimal approvalRate,
        BigDecimal rejectionRate,
        BigDecimal rewardAmount
) {
}
