package com.labelhub.modules.admin.dashboard.dto;

import com.labelhub.common.exception.BusinessException;

public enum AdminDashboardRange {
    LAST_7_DAYS("7d", 7),
    LAST_30_DAYS("30d", 30);

    private final String code;
    private final int days;

    AdminDashboardRange(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String code() {
        return code;
    }

    public int days() {
        return days;
    }

    public static AdminDashboardRange from(String value) {
        if (value == null || value.isBlank() || LAST_7_DAYS.code.equals(value)) {
            return LAST_7_DAYS;
        }
        if (LAST_30_DAYS.code.equals(value)) {
            return LAST_30_DAYS;
        }
        throw new BusinessException(400102, "range 只支持 7d 或 30d");
    }
}
