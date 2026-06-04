package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端看板异常提醒级别")
public enum AdminDashboardAlertLevel {
    INFO,
    WARNING,
    CRITICAL
}
