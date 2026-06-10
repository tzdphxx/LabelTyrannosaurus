package com.labelhub.modules.role.dashboard;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.role.dashboard.controller.ReviewerDashboardController;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.ReviewerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.service.ReviewerDashboardService;
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

class ReviewerDashboardControllerTest {

    private final ReviewerDashboardService service = mock(ReviewerDashboardService.class);
    private final ReviewerDashboardController controller = new ReviewerDashboardController(service);

    @Test
    void overviewWithoutRangeUsesSevenDays() {
        when(service.getOverview(DashboardRange.LAST_7_DAYS)).thenReturn(response("7d"));

        var apiResponse = controller.overview(null, new MockHttpServletRequest());

        assertThat(apiResponse.data().range()).isEqualTo("7d");
        verify(service).getOverview(DashboardRange.LAST_7_DAYS);
    }

    @Test
    void overviewRejectsReviewerIdQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("reviewerId", "30");

        assertThatThrownBy(() -> controller.overview("7d", request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void controllerRequiresReviewerRole() {
        PreAuthorize annotation = ReviewerDashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('REVIEWER')");
    }

    private static ReviewerDashboardOverviewResponse response(String range) {
        return new ReviewerDashboardOverviewResponse(
                range,
                new ReviewerDashboardOverviewResponse.QueueSummary(0, 0, 0, 0),
                new ReviewerDashboardOverviewResponse.ReviewerKpis(0, 0, 0, BigDecimal.ZERO, 0),
                List.of(),
                new ReviewerDashboardOverviewResponse.AiReviewSummary(0, 0, 0, 0),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );
    }
}
