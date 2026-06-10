package com.labelhub.modules.role.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardRangeTest {

    @Test
    void blankValueUsesCallerProvidedDefault() {
        assertThat(DashboardRange.from(null, DashboardRange.LAST_30_DAYS))
                .isEqualTo(DashboardRange.LAST_30_DAYS);
        assertThat(DashboardRange.from("", DashboardRange.LAST_7_DAYS))
                .isEqualTo(DashboardRange.LAST_7_DAYS);
    }

    @Test
    void sevenAndThirtyDayRangesAreSupported() {
        assertThat(DashboardRange.from("7d", DashboardRange.LAST_30_DAYS))
                .isEqualTo(DashboardRange.LAST_7_DAYS);
        assertThat(DashboardRange.from("30d", DashboardRange.LAST_7_DAYS))
                .isEqualTo(DashboardRange.LAST_30_DAYS);
        assertThat(DashboardRange.LAST_7_DAYS.days()).isEqualTo(7);
        assertThat(DashboardRange.LAST_30_DAYS.code()).isEqualTo("30d");
    }

    @Test
    void unsupportedValueThrowsParameterError() {
        assertThatThrownBy(() -> DashboardRange.from("90d", DashboardRange.LAST_7_DAYS))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }
}
