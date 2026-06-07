package com.labelhub.modules.admin.dashboard;

import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlertType;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardRange;
import com.labelhub.modules.admin.dashboard.mapper.AdminDashboardMapper;
import com.labelhub.modules.admin.dashboard.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    private final AdminDashboardMapper mapper = mock(AdminDashboardMapper.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-03T10:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );
    private final AdminDashboardService service = new AdminDashboardService(mapper, clock);

    @BeforeEach
    void setUp() {
        when(mapper.countActiveTasks(any(), any())).thenReturn(0L);
        when(mapper.countClaimed(any(), any())).thenReturn(0L);
        when(mapper.countSubmitted(any(), any())).thenReturn(0L);
        when(mapper.countPendingReview()).thenReturn(0L);
        when(mapper.selectReviewCounts(any(), any())).thenReturn(new AdminDashboardMapper.ReviewCountRow(0L, 0L));
        when(mapper.sumRewardAmount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(mapper.countTotalUsers()).thenReturn(0L);
        when(mapper.countDisabledUsers()).thenReturn(0L);
        when(mapper.countNewUsers(any(), any())).thenReturn(0L);
        when(mapper.selectRoleCounts()).thenReturn(List.of());
        when(mapper.selectSubmittedTrend(any(), any())).thenReturn(List.of());
        when(mapper.selectReviewTrend(any(), any())).thenReturn(List.of());
        when(mapper.selectRewardTrend(any(), any())).thenReturn(List.of());
        when(mapper.selectTaskStatusDistribution()).thenReturn(List.of());
        when(mapper.selectTopLabelers(any(), any(), anyInt())).thenReturn(List.of());
        when(mapper.selectTopTasks(any(), any(), anyInt())).thenReturn(List.of());
        when(mapper.existsOverduePendingReview(any())).thenReturn(false);
        when(mapper.selectHighRejectionRateTasks(any(), any())).thenReturn(List.of());
        when(mapper.selectZeroSubmissionActiveTasks(any(), any())).thenReturn(List.of());
    }

    @Test
    void emptyDashboardReturnsZeroValuesAndFilledTrend() {
        var response = service.getOverview(AdminDashboardRange.LAST_7_DAYS);

        assertThat(response.range()).isEqualTo("7d");
        assertThat(response.kpis().activeTaskCount()).isZero();
        assertThat(response.kpis().approvalRate()).isEqualByComparingTo("0");
        assertThat(response.trend()).hasSize(7);
        assertThat(response.trend().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 28));
        assertThat(response.trend().get(6).date()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(response.userSummary().roleCounts())
                .containsEntry("ADMIN", 0L)
                .containsEntry("OWNER", 0L)
                .containsEntry("LABELER", 0L)
                .containsEntry("REVIEWER", 0L);
        assertThat(response.taskStatusDistribution())
                .containsEntry("DRAFT", 0L)
                .containsEntry("PUBLISHED", 0L)
                .containsEntry("PAUSED", 0L)
                .containsEntry("ENDED", 0L);
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void approvalAndRejectionRateUseReviewedCountAsDenominator() {
        when(mapper.selectReviewCounts(any(), any())).thenReturn(new AdminDashboardMapper.ReviewCountRow(8L, 2L));

        var response = service.getOverview(AdminDashboardRange.LAST_7_DAYS);

        assertThat(response.kpis().approvalRate()).isEqualByComparingTo("0.8000");
        assertThat(response.kpis().rejectionRate()).isEqualByComparingTo("0.2000");
    }

    @Test
    void trendRowsAreMergedAndMissingDatesAreFilledWithZero() {
        when(mapper.selectSubmittedTrend(any(), any())).thenReturn(List.of(
                new AdminDashboardMapper.DateCountRow(LocalDate.of(2026, 6, 1), 2L)
        ));
        when(mapper.selectReviewTrend(any(), any())).thenReturn(List.of(
                new AdminDashboardMapper.DateReviewCountRow(LocalDate.of(2026, 6, 2), 1L, 1L)
        ));
        when(mapper.selectRewardTrend(any(), any())).thenReturn(List.of(
                new AdminDashboardMapper.DateRewardRow(LocalDate.of(2026, 6, 3), new BigDecimal("3.50"))
        ));

        var response = service.getOverview(AdminDashboardRange.LAST_7_DAYS);

        assertThat(response.trend())
                .filteredOn(point -> point.date().equals(LocalDate.of(2026, 6, 1)))
                .singleElement()
                .extracting("submittedCount")
                .isEqualTo(2L);
        assertThat(response.trend())
                .filteredOn(point -> point.date().equals(LocalDate.of(2026, 6, 2)))
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.approvedCount()).isEqualTo(1L);
                    assertThat(point.rejectedCount()).isEqualTo(1L);
                });
        assertThat(response.trend())
                .filteredOn(point -> point.date().equals(LocalDate.of(2026, 6, 3)))
                .singleElement()
                .extracting("rewardAmount")
                .isEqualTo(new BigDecimal("3.50"));
    }

    @Test
    void alertsAreGeneratedWhenThresholdsMatch() {
        when(mapper.countPendingReview()).thenReturn(20L);
        when(mapper.countDisabledUsers()).thenReturn(3L);
        when(mapper.selectHighRejectionRateTasks(any(), any())).thenReturn(List.of(
                new AdminDashboardMapper.HighRejectionTaskRow(1001L, "质检任务", 5L, 5L)
        ));
        when(mapper.selectZeroSubmissionActiveTasks(any(), any())).thenReturn(List.of(
                new AdminDashboardMapper.ZeroSubmissionTaskRow(1002L, "冷启动任务", 3L)
        ));

        var response = service.getOverview(AdminDashboardRange.LAST_7_DAYS);

        assertThat(response.alerts())
                .extracting("type")
                .contains(
                        AdminDashboardAlertType.REVIEW_BACKLOG,
                        AdminDashboardAlertType.DISABLED_USER,
                        AdminDashboardAlertType.HIGH_REJECTION_RATE_TASK,
                        AdminDashboardAlertType.ZERO_SUBMISSION_ACTIVE_TASK
                );
    }
}
