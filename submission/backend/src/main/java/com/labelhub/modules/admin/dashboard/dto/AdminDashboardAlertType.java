package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端看板异常提醒类型")
public enum AdminDashboardAlertType {
    REVIEW_BACKLOG,
    HIGH_REJECTION_RATE_TASK,
    ZERO_SUBMISSION_ACTIVE_TASK,
    DISABLED_USER
}
