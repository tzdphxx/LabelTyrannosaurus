package com.labelhub.modules.admin.dashboard.dto;

public record AdminDashboardTopTask(
        Long taskId,
        String title,
        long submittedCount,
        long approvedCount,
        long rejectedCount
) {
}
