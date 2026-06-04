package com.labelhub.modules.role.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.role.dashboard.controller.OwnerDashboardController;
import com.labelhub.modules.role.dashboard.dto.OwnerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.dto.TrendDays;
import com.labelhub.modules.role.dashboard.service.OwnerDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OwnerDashboardControllerTest {

    private final OwnerDashboardService service = mock(OwnerDashboardService.class);
    private final OwnerDashboardController controller = new OwnerDashboardController(service);

    @Test
    void overviewWithoutTrendDaysUsesThirtyDays() {
        when(service.getOverview(TrendDays.LAST_30_DAYS)).thenReturn(response(30));

        var apiResponse = controller.overview(null, new MockHttpServletRequest());

        assertThat(apiResponse.data().trendDays()).isEqualTo(30);
        verify(service).getOverview(TrendDays.LAST_30_DAYS);
    }

    @Test
    void overviewRejectsUserIdQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "99");

        assertThatThrownBy(() -> controller.overview(7, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void controllerRequiresOwnerRole() {
        PreAuthorize annotation = OwnerDashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('OWNER')");
    }

    private static OwnerDashboardOverviewResponse response(int trendDays) {
        return new OwnerDashboardOverviewResponse(
                trendDays,
                new OwnerDashboardOverviewResponse.OwnerKpis(0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO),
                Map.of(),
                List.of(),
                new OwnerDashboardOverviewResponse.QualitySummary(0, 0, BigDecimal.ZERO),
                new OwnerDashboardOverviewResponse.RewardSummary(BigDecimal.ZERO, 0),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );
    }
}
