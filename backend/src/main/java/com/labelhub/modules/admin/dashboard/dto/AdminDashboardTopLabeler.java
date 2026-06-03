package com.labelhub.modules.admin.dashboard.dto;

import java.math.BigDecimal;

public record AdminDashboardTopLabeler(
        Long labelerId,
        String displayName,
        long submittedCount,
        long approvedCount,
        BigDecimal rewardAmount
) {
}
