package com.labelhub.modules.role.dashboard.dto;

import com.labelhub.common.exception.BusinessException;

public enum TrendDays {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30);

    private final int days;

    TrendDays(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    public static TrendDays from(Integer value) {
        if (value == null || value == 30) {
            return LAST_30_DAYS;
        }
        if (value == 7) {
            return LAST_7_DAYS;
        }
        throw new BusinessException(400102, "trendDays 只支持 7 或 30");
    }
}
