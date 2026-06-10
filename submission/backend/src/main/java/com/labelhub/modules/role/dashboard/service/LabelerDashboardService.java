package com.labelhub.modules.role.dashboard.service;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.role.dashboard.dto.DashboardAlertLevel;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.LabelerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.mapper.LabelerDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabelerDashboardService {

    private static final int LIST_LIMIT = 5;

    private final LabelerDashboardMapper mapper;
    private final Clock clock;

    @Autowired
    public LabelerDashboardService(LabelerDashboardMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    public LabelerDashboardService(LabelerDashboardMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public LabelerDashboardOverviewResponse getOverview(DashboardRange range) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        OwnerDashboardService.requireExactRole(currentUser, RoleCode.LABELER, "当前账号没有权限查看 Labeler 看板");
        Long labelerId = currentUser.userId();

        OwnerDashboardService.DateWindow window = OwnerDashboardService.DateWindow.of(range.days(), clock);
        LabelerDashboardMapper.SubmissionCountRow submissionCounts =
                mapper.selectSubmissionCounts(labelerId, window.startAt(), window.endExclusive());
        long submittedCount = submissionCounts == null ? 0L : OwnerDashboardService.value(submissionCounts.submittedCount());
        long approvedCount = submissionCounts == null ? 0L : OwnerDashboardService.value(submissionCounts.approvedCount());
        long rejectedCount = submissionCounts == null ? 0L : OwnerDashboardService.value(submissionCounts.rejectedCount());
        long reviewedCount = approvedCount + rejectedCount;
        long rejectedNeedFixCount = OwnerDashboardService.value(mapper.countRejectedNeedFix(labelerId));

        LabelerDashboardOverviewResponse.LabelerKpis kpis = new LabelerDashboardOverviewResponse.LabelerKpis(
                OwnerDashboardService.value(mapper.countClaimed(labelerId, window.startAt(), window.endExclusive())),
                submittedCount,
                approvedCount,
                rejectedCount,
                OwnerDashboardService.rate(approvedCount, reviewedCount),
                OwnerDashboardService.amount(mapper.sumPeriodReward(labelerId, window.startAt(), window.endExclusive())),
                OwnerDashboardService.amount(mapper.sumTotalReward(labelerId)),
                rejectedNeedFixCount
        );

        LabelerDashboardOverviewResponse.TodoSummary todoSummary = new LabelerDashboardOverviewResponse.TodoSummary(
                OwnerDashboardService.value(mapper.countClaimedNotSubmitted(labelerId)),
                rejectedNeedFixCount,
                OwnerDashboardService.value(mapper.countContinuableTasks(labelerId))
        );

        return new LabelerDashboardOverviewResponse(
                range.code(),
                kpis,
                buildTrend(labelerId, window),
                OwnerDashboardService.list(mapper.selectTaskContributions(labelerId, window.startAt(), window.endExclusive(), LIST_LIMIT)),
                todoSummary,
                buildAlerts(labelerId, todoSummary),
                LocalDateTime.now(clock)
        );
    }

    private List<LabelerDashboardOverviewResponse.ContributionTrendPoint> buildTrend(Long labelerId,
                                                                                     OwnerDashboardService.DateWindow window) {
        Map<LocalDate, Long> submitted = dateCounts(mapper.selectSubmittedTrend(labelerId, window.startAt(), window.endExclusive()));
        Map<LocalDate, Long> approved = dateCounts(mapper.selectApprovedTrend(labelerId, window.startAt(), window.endExclusive()));
        Map<LocalDate, BigDecimal> rewards = rewardByDate(mapper.selectRewardTrend(labelerId, window.startAt(), window.endExclusive()));

        List<LabelerDashboardOverviewResponse.ContributionTrendPoint> trend = new ArrayList<>();
        for (LocalDate date = window.startDate(); !date.isAfter(window.endDate()); date = date.plusDays(1)) {
            trend.add(new LabelerDashboardOverviewResponse.ContributionTrendPoint(
                    date,
                    submitted.getOrDefault(date, 0L),
                    approved.getOrDefault(date, 0L),
                    rewards.getOrDefault(date, BigDecimal.ZERO)
            ));
        }
        return trend;
    }

    private List<LabelerDashboardOverviewResponse.Alert> buildAlerts(Long labelerId,
                                                                     LabelerDashboardOverviewResponse.TodoSummary todoSummary) {
        List<LabelerDashboardOverviewResponse.Alert> alerts = new ArrayList<>();
        if (todoSummary.rejectedNeedFixCount() > 0) {
            alerts.add(new LabelerDashboardOverviewResponse.Alert(
                    "REJECTED_SUBMISSION",
                    DashboardAlertLevel.WARNING,
                    "存在被打回提交",
                    "当前有 " + todoSummary.rejectedNeedFixCount() + " 条提交需要修改后重新提交",
                    "/app/labeler/submissions?status=REJECTED"
            ));
        }
        if (todoSummary.claimedNotSubmittedCount() > 0) {
            alerts.add(new LabelerDashboardOverviewResponse.Alert(
                    "CLAIMED_NOT_SUBMITTED",
                    DashboardAlertLevel.INFO,
                    "存在已领取未提交任务",
                    "当前有 " + todoSummary.claimedNotSubmittedCount() + " 条领取记录尚未提交",
                    "/app/labeler/submissions"
            ));
        }

        OwnerDashboardService.DateWindow recentWindow = OwnerDashboardService.DateWindow.of(7, clock);
        if (OwnerDashboardService.value(mapper.countRecentSubmitted(
                labelerId, recentWindow.startAt(), recentWindow.endExclusive())) == 0L) {
            alerts.add(new LabelerDashboardOverviewResponse.Alert(
                    "NO_RECENT_SUBMISSION",
                    DashboardAlertLevel.INFO,
                    "近 7 天无提交",
                    "近 7 天暂无提交记录",
                    "/app/labeler/market"
            ));
        }
        long rewardNotVisibleCount = OwnerDashboardService.value(mapper.countRewardNotVisibleTasks(labelerId));
        if (rewardNotVisibleCount > 0) {
            alerts.add(new LabelerDashboardOverviewResponse.Alert(
                    "REWARD_NOT_VISIBLE",
                    DashboardAlertLevel.INFO,
                    "存在奖励不可见任务",
                    "当前有 " + rewardNotVisibleCount + " 个参与任务未展示奖励",
                    "/app/labeler/submissions"
            ));
        }
        return alerts.stream().limit(LIST_LIMIT).toList();
    }

    private static Map<LocalDate, Long> dateCounts(List<LabelerDashboardMapper.DateCountRow> rows) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (LabelerDashboardMapper.DateCountRow row : OwnerDashboardService.list(rows)) {
            counts.put(row.statDate(), OwnerDashboardService.value(row.count()));
        }
        return counts;
    }

    private static Map<LocalDate, BigDecimal> rewardByDate(List<LabelerDashboardMapper.DateRewardRow> rows) {
        Map<LocalDate, BigDecimal> rewards = new LinkedHashMap<>();
        for (LabelerDashboardMapper.DateRewardRow row : OwnerDashboardService.list(rows)) {
            rewards.put(row.statDate(), OwnerDashboardService.amount(row.reward()));
        }
        return rewards;
    }
}
