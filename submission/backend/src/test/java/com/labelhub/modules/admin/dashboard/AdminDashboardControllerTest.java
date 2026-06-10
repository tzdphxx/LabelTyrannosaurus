package com.labelhub.modules.admin.dashboard;

import com.labelhub.modules.admin.dashboard.controller.AdminDashboardController;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardKpis;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardOverviewResponse;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardRange;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardUserSummary;
import com.labelhub.modules.admin.dashboard.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
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

class AdminDashboardControllerTest {

    private final AdminDashboardService adminDashboardService = mock(AdminDashboardService.class);
    private final AdminDashboardController controller = new AdminDashboardController(adminDashboardService);

    @Test
    void overviewWithoutRangeUsesDefault7d() {
        when(adminDashboardService.getOverview(AdminDashboardRange.LAST_7_DAYS)).thenReturn(response("7d"));

        var apiResponse = controller.overview(null);

        assertThat(apiResponse.data().range()).isEqualTo("7d");
        verify(adminDashboardService).getOverview(AdminDashboardRange.LAST_7_DAYS);
    }

    @Test
    void overviewWith30dUsesLast30Days() {
        when(adminDashboardService.getOverview(AdminDashboardRange.LAST_30_DAYS)).thenReturn(response("30d"));

        var apiResponse = controller.overview("30d");

        assertThat(apiResponse.data().range()).isEqualTo("30d");
        verify(adminDashboardService).getOverview(AdminDashboardRange.LAST_30_DAYS);
    }

    @Test
    void overviewWithInvalidRangeThrowsBusinessException() {
        assertThatThrownBy(() -> controller.overview("90d"))
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void controllerRequiresAdminRole() {
        var annotation = AdminDashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    private static AdminDashboardOverviewResponse response(String range) {
        return new AdminDashboardOverviewResponse(
                range,
                new AdminDashboardKpis(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AdminDashboardUserSummary(0, Map.of(), 0, 0),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );
    }
}
