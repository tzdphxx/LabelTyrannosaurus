package com.labelhub.modules.role.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.role.dashboard.controller.LabelerDashboardController;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.LabelerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.service.LabelerDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelerDashboardControllerTest {

    private final LabelerDashboardService service = mock(LabelerDashboardService.class);
    private final LabelerDashboardController controller = new LabelerDashboardController(service);

    @Test
    void overviewWithoutRangeUsesThirtyDays() {
        when(service.getOverview(DashboardRange.LAST_30_DAYS)).thenReturn(response("30d"));

        var apiResponse = controller.overview(null, new MockHttpServletRequest());

        assertThat(apiResponse.data().range()).isEqualTo("30d");
        verify(service).getOverview(DashboardRange.LAST_30_DAYS);
    }

    @Test
    void overviewRejectsLabelerIdQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("labelerId", "20");

        assertThatThrownBy(() -> controller.overview("7d", request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void controllerRequiresLabelerRole() {
        PreAuthorize annotation = LabelerDashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('LABELER')");
    }

    private static LabelerDashboardOverviewResponse response(String range) {
        return new LabelerDashboardOverviewResponse(
                range,
                new LabelerDashboardOverviewResponse.LabelerKpis(
                        0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0),
                List.of(),
                List.of(),
                new LabelerDashboardOverviewResponse.TodoSummary(0, 0, 0),
                List.of(),
                LocalDateTime.now()
        );
    }
}
