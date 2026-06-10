package com.labelhub.modules.role.dashboard;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.mapper.LabelerDashboardMapper;
import com.labelhub.modules.role.dashboard.service.LabelerDashboardService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelerDashboardServiceTest {

    private final LabelerDashboardMapper mapper = mock(LabelerDashboardMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private final LabelerDashboardService service = new LabelerDashboardService(mapper, clock);

    @BeforeEach
    void setUp() {
        CurrentUserContext.set(user(20L, RoleCode.LABELER));
        when(mapper.countClaimed(eq(20L), any(), any())).thenReturn(0L);
        when(mapper.selectSubmissionCounts(eq(20L), any(), any()))
                .thenReturn(new LabelerDashboardMapper.SubmissionCountRow(0L, 0L, 0L));
        when(mapper.sumPeriodReward(eq(20L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(mapper.sumTotalReward(20L)).thenReturn(BigDecimal.ZERO);
        when(mapper.countClaimedNotSubmitted(20L)).thenReturn(0L);
        when(mapper.countRejectedNeedFix(20L)).thenReturn(0L);
        when(mapper.countContinuableTasks(20L)).thenReturn(0L);
        when(mapper.countRecentSubmitted(eq(20L), any(), any())).thenReturn(0L);
        when(mapper.countRewardNotVisibleTasks(20L)).thenReturn(0L);
        when(mapper.selectSubmittedTrend(eq(20L), any(), any())).thenReturn(List.of());
        when(mapper.selectApprovedTrend(eq(20L), any(), any())).thenReturn(List.of());
        when(mapper.selectRewardTrend(eq(20L), any(), any())).thenReturn(List.of());
        when(mapper.selectTaskContributions(eq(20L), any(), any(), eq(5))).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void emptyDashboardReturnsZeroValuesAndFilledTrend() {
        var response = service.getOverview(DashboardRange.LAST_7_DAYS);

        assertThat(response.range()).isEqualTo("7d");
        assertThat(response.kpis().approvalRate()).isEqualByComparingTo("0");
        assertThat(response.contributionTrend()).hasSize(7);
        assertThat(response.contributionTrend().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 28));
        assertThat(response.alerts())
                .extracting("type")
                .contains("NO_RECENT_SUBMISSION");
        verify(mapper).countClaimed(eq(20L), any(), any());
    }

    @Test
    void adminWithoutLabelerRoleIsRejectedByExactRoleCheck() {
        CurrentUserContext.set(user(1L, RoleCode.ADMIN));

        assertThatThrownBy(() -> service.getOverview(DashboardRange.LAST_30_DAYS))
                .extracting("code")
                .isEqualTo(403001);
    }

    private static CurrentUser user(Long id, RoleCode role) {
        return new CurrentUser(id, role.name().toLowerCase(), "test@labelhub.dev", Set.of(role), 1);
    }
}
