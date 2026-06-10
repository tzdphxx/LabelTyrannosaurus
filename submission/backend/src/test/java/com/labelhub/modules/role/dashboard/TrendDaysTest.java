package com.labelhub.modules.role.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.role.dashboard.dto.TrendDays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrendDaysTest {

    @Test
    void nullUsesLastThirtyDays() {
        assertThat(TrendDays.from(null)).isEqualTo(TrendDays.LAST_30_DAYS);
        assertThat(TrendDays.LAST_30_DAYS.days()).isEqualTo(30);
    }

    @Test
    void sevenAndThirtyDaysAreSupported() {
        assertThat(TrendDays.from(7)).isEqualTo(TrendDays.LAST_7_DAYS);
        assertThat(TrendDays.from(30)).isEqualTo(TrendDays.LAST_30_DAYS);
    }

    @Test
    void unsupportedValueThrowsParameterError() {
        assertThatThrownBy(() -> TrendDays.from(90))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }
}
