package com.labelhub.modules.admin.dashboard.dto;

import java.util.Map;

public record AdminDashboardUserSummary(
        long totalUserCount,
        Map<String, Long> roleCounts,
        long disabledUserCount,
        long newUserCount
) {
}
