package com.labelhub.modules.admin.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminDashboardRangeTest {

    @Test
    void defaultRangeUsesLastSevenDays() {
        assertThat(AdminDashboardRange.from(null)).isEqualTo(AdminDashboardRange.LAST_7_DAYS);
        assertThat(AdminDashboardRange.from("")).isEqualTo(AdminDashboardRange.LAST_7_DAYS);
        assertThat(AdminDashboardRange.from("7d")).isEqualTo(AdminDashboardRange.LAST_7_DAYS);
        assertThat(AdminDashboardRange.LAST_7_DAYS.code()).isEqualTo("7d");
        assertThat(AdminDashboardRange.LAST_7_DAYS.days()).isEqualTo(7);
    }

    @Test
    void thirtyDayRangeIsSupported() {
        assertThat(AdminDashboardRange.from("30d")).isEqualTo(AdminDashboardRange.LAST_30_DAYS);
        assertThat(AdminDashboardRange.LAST_30_DAYS.code()).isEqualTo("30d");
        assertThat(AdminDashboardRange.LAST_30_DAYS.days()).isEqualTo(30);
    }

    @Test
    void invalidRangeThrowsBusinessException() {
        assertThatThrownBy(() -> AdminDashboardRange.from("90d"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }
}
