package com.labelhub.modules.admin.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminDashboardTrendPoint(
        LocalDate date,
        long submittedCount,
        long approvedCount,
        long rejectedCount,
        BigDecimal rewardAmount
) {
}
