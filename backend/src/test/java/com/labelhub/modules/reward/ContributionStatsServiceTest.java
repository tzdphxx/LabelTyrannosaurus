package com.labelhub.modules.reward;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.reward.domain.LabelerContributionStatsEntity;
import com.labelhub.modules.reward.dto.DailyContributionPoint;
import com.labelhub.modules.reward.repository.ContributionStatsMapper;
import com.labelhub.modules.reward.service.ContributionStatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContributionStatsServiceTest {

    private final ContributionStatsMapper contributionStatsMapper = mock(ContributionStatsMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-08T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private final ContributionStatsService contributionStatsService =
            new ContributionStatsService(contributionStatsMapper, clock);

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void overviewUsesReviewedCountAsApprovalRateDenominator() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "labeler@example.com", Set.of(RoleCode.LABELER), 1));
        LabelerContributionStatsEntity stats = new LabelerContributionStatsEntity();
        stats.setLabelerId(20L);
        stats.setSubmittedCount(10);
        stats.setPendingReviewCount(6);
        stats.setApprovedCount(3);
        stats.setRejectedCount(1);
        stats.setTotalReward(new BigDecimal("9.00"));
        when(contributionStatsMapper.selectOverviewByLabelerId(20L)).thenReturn(stats);

        var response = contributionStatsService.getOverview();

        assertThat(response.submittedCount()).isEqualTo(10);
        assertThat(response.pendingReviewCount()).isEqualTo(6);
        assertThat(response.approvalRate()).isEqualByComparingTo("0.7500");
    }

    @Test
    void sevenDayTrendFillsMissingDatesWithZero() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "labeler@example.com", Set.of(RoleCode.LABELER), 1));
        when(contributionStatsMapper.selectDailyTrend(20L, LocalDate.parse("2026-05-02"), LocalDate.parse("2026-05-08")))
                .thenReturn(List.of(new DailyContributionPoint(
                        LocalDate.parse("2026-05-06"), 2, 1, 0, new BigDecimal("3.00"))));

        var trend = contributionStatsService.getTrend(7);

        assertThat(trend).hasSize(7);
        assertThat(trend.get(0).statDate()).isEqualTo(LocalDate.parse("2026-05-02"));
        assertThat(trend.get(0).rewardAmount()).isEqualByComparingTo("0.00");
        assertThat(trend.get(4).statDate()).isEqualTo(LocalDate.parse("2026-05-06"));
        assertThat(trend.get(4).approvedCount()).isEqualTo(1);
        assertThat(trend.get(6).statDate()).isEqualTo(LocalDate.parse("2026-05-08"));
    }
}
