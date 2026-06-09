package com.labelhub.modules.admin.dto;

import java.math.BigDecimal;

public record AssignableReviewerResponse(
        Long reviewerId,
        String username,
        String email,
        Boolean enabled,
        Boolean loginEnabled,
        Long pendingCount,
        Long todayReviewedCount,
        Long totalApprovedCount,
        Long totalRejectedCount,
        BigDecimal approvalRate
) {
}
