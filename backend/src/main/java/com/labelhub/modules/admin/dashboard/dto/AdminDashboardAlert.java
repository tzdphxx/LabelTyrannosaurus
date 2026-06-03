package com.labelhub.modules.admin.dashboard.dto;

public record AdminDashboardAlert(
        AdminDashboardAlertType type,
        AdminDashboardAlertLevel level,
        String title,
        String description,
        String targetPath
) {
}
