package com.labelhub.modules.review.dto;

import java.math.BigDecimal;

public record ReviewerDashboardResponse(
        int pendingCount,
        int todayReviewedCount,
        int totalApproved,
        int totalRejected,
        BigDecimal approvalRate
) {
}
