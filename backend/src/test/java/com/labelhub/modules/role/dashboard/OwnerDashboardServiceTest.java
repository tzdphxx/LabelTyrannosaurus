package com.labelhub.modules.role.dashboard;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.role.dashboard.dto.DashboardAlertLevel;
import com.labelhub.modules.role.dashboard.dto.TrendDays;
import com.labelhub.modules.role.dashboard.mapper.OwnerDashboardMapper;
import com.labelhub.modules.role.dashboard.service.OwnerDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OwnerDashboardServiceTest {

    private final OwnerDashboardMapper mapper = mock(OwnerDashboardMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private final OwnerDashboardService service = new OwnerDashboardService(mapper, clock);

    @BeforeEach
    void setUp() {
        CurrentUserContext.set(user(10L, RoleCode.OWNER));
        when(mapper.countTotalTasks(10L)).thenReturn(0L);
        when(mapper.countRunningTasks(10L)).thenReturn(0L);
        when(mapper.countClaimedItems(10L)).thenReturn(0L);
        when(mapper.countSubmittedItems(10L)).thenReturn(0L);
        when(mapper.countPendingReview(10L)).thenReturn(0L);
        when(mapper.selectReviewCounts(10L)).thenReturn(new OwnerDashboardMapper.ReviewCountRow(0L, 0L));
        when(mapper.sumRewardCost(10L)).thenReturn(BigDecimal.ZERO);
        when(mapper.countRewardVisibleTasks(10L)).thenReturn(0L);
        when(mapper.selectTaskStatusDistribution(10L)).thenReturn(List.of());
        when(mapper.selectClaimedTrend(eq(10L), any(), any())).thenReturn(List.of());
        when(mapper.selectSubmittedTrend(eq(10L), any(), any())).thenReturn(List.of());
        when(mapper.selectApprovedTrend(eq(10L), any(), any())).thenReturn(List.of());
        when(mapper.selectAttentionTaskCandidates(eq(10L), any())).thenReturn(List.of());
        when(mapper.selectRecentTasks(10L, 5)).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void emptyDashboardReturnsZeroValuesAndFilledTrend() {
        var response = service.getOverview(TrendDays.LAST_7_DAYS);

        assertThat(response.trendDays()).isEqualTo(7);
        assertThat(response.kpis().approvalRate()).isEqualByComparingTo("0");
        assertThat(response.deliveryTrend()).hasSize(7);
        assertThat(response.deliveryTrend().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 28));
        assertThat(response.deliveryTrend().get(6).date()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(response.taskStatusDistribution())
                .containsEntry("DRAFT", 0L)
                .containsEntry("PUBLISHED", 0L)
                .containsEntry("PAUSED", 0L)
                .containsEntry("ENDED", 0L);
        verify(mapper).countTotalTasks(10L);
    }

    @Test
    void adminWithoutOwnerRoleIsRejectedByExactRoleCheck() {
        CurrentUserContext.set(user(1L, RoleCode.ADMIN));

        assertThatThrownBy(() -> service.getOverview(TrendDays.LAST_7_DAYS))
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void reviewBacklogAttentionTaskIsGenerated() {
        when(mapper.selectAttentionTaskCandidates(eq(10L), any())).thenReturn(List.of(
                new OwnerDashboardMapper.AttentionTaskRow(
                        1001L, "质检任务", 50L, 100L, 12L, 8L, 1L,
                        LocalDate.of(2026, 6, 1).atStartOfDay(),
                        LocalDate.of(2026, 6, 3).atStartOfDay())
        ));

        var response = service.getOverview(TrendDays.LAST_7_DAYS);

        assertThat(response.attentionTasks())
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.type()).isEqualTo("REVIEW_BACKLOG");
                    assertThat(alert.level()).isEqualTo(DashboardAlertLevel.WARNING);
                    assertThat(alert.targetPath()).isEqualTo("/app/owner/tasks/1001/edit");
                });
    }

    private static CurrentUser user(Long id, RoleCode role) {
        return new CurrentUser(id, role.name().toLowerCase(), "test@labelhub.dev", Set.of(role), 1);
    }
}
