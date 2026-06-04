package com.labelhub.modules.role.dashboard.dto;

import com.labelhub.common.exception.BusinessException;

public enum DashboardRange {
    LAST_7_DAYS("7d", 7),
    LAST_30_DAYS("30d", 30);

    private final String code;
    private final int days;

    DashboardRange(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String code() {
        return code;
    }

    public int days() {
        return days;
    }

    public static DashboardRange from(String value, DashboardRange defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        if ("7d".equals(value)) {
            return LAST_7_DAYS;
        }
        if ("30d".equals(value)) {
            return LAST_30_DAYS;
        }
        throw new BusinessException(400102, "range 只支持 7d 或 30d");
    }
}
