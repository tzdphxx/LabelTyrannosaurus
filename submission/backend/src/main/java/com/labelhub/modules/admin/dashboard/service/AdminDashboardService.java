package com.labelhub.modules.admin.dashboard.service;

import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlert;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlertLevel;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlertType;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardKpis;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardOverviewResponse;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardRange;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopLabeler;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopTask;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTrendPoint;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardUserSummary;
import com.labelhub.modules.admin.dashboard.mapper.AdminDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final int TOP_LIMIT = 5;
    private static final long REVIEW_BACKLOG_THRESHOLD = 20L;
    private static final int REVIEW_TIMEOUT_HOURS = 24;
    private static final BigDecimal HIGH_REJECTION_RATE_THRESHOLD = new BigDecimal("0.5000");
    private static final List<String> ROLE_KEYS = List.of("ADMIN", "OWNER", "LABELER", "REVIEWER");
    private static final List<String> TASK_STATUS_KEYS = List.of("DRAFT", "PUBLISHED", "PAUSED", "ENDED");

    private final AdminDashboardMapper mapper;
    private final Clock clock;

    @Autowired
    public AdminDashboardService(AdminDashboardMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    public AdminDashboardService(AdminDashboardMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public AdminDashboardOverviewResponse getOverview(AdminDashboardRange range) {
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(range.days() - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        long activeTaskCount = value(mapper.countActiveTasks(startAt, endExclusive));
        long claimedCount = value(mapper.countClaimed(startAt, endExclusive));
        long submittedCount = value(mapper.countSubmitted(startAt, endExclusive));
        long pendingReviewCount = value(mapper.countPendingReview());
        AdminDashboardMapper.ReviewCountRow reviewCounts = mapper.selectReviewCounts(startAt, endExclusive);
        long approvedCount = reviewCounts == null ? 0L : value(reviewCounts.approvedCount());
        long rejectedCount = reviewCounts == null ? 0L : value(reviewCounts.rejectedCount());
        BigDecimal rewardAmount = amount(mapper.sumRewardAmount(startAt, endExclusive));

        AdminDashboardKpis kpis = new AdminDashboardKpis(
                activeTaskCount,
                claimedCount,
                submittedCount,
                pendingReviewCount,
                rate(approvedCount, approvedCount + rejectedCount),
                rate(rejectedCount, approvedCount + rejectedCount),
                rewardAmount
        );

        AdminDashboardUserSummary userSummary = new AdminDashboardUserSummary(
                value(mapper.countTotalUsers()),
                fillCounts(ROLE_KEYS, mapper.selectRoleCounts()),
                value(mapper.countDisabledUsers()),
                value(mapper.countNewUsers(startAt, endExclusive))
        );

        Map<String, Long> taskStatusDistribution = fillCounts(TASK_STATUS_KEYS, mapper.selectTaskStatusDistribution());
        List<AdminDashboardTrendPoint> trend = buildTrend(startDate, endDate, startAt, endExclusive);
        List<AdminDashboardTopLabeler> topLabelers = list(mapper.selectTopLabelers(startAt, endExclusive, TOP_LIMIT));
        List<AdminDashboardTopTask> topTasks = list(mapper.selectTopTasks(startAt, endExclusive, TOP_LIMIT));
        List<AdminDashboardAlert> alerts = buildAlerts(startAt, endExclusive, pendingReviewCount, userSummary.disabledUserCount());

        return new AdminDashboardOverviewResponse(
                range.code(),
                kpis,
                userSummary,
                trend,
                taskStatusDistribution,
                topLabelers,
                topTasks,
                alerts,
                LocalDateTime.now(clock)
        );
    }

    private List<AdminDashboardTrendPoint> buildTrend(LocalDate startDate,
                                                      LocalDate endDate,
                                                      LocalDateTime startAt,
                                                      LocalDateTime endExclusive) {
        Map<LocalDate, Long> submittedByDate = new LinkedHashMap<>();
        for (AdminDashboardMapper.DateCountRow row : list(mapper.selectSubmittedTrend(startAt, endExclusive))) {
            submittedByDate.put(row.statDate(), value(row.count()));
        }

        Map<LocalDate, AdminDashboardMapper.DateReviewCountRow> reviewedByDate = new LinkedHashMap<>();
        for (AdminDashboardMapper.DateReviewCountRow row : list(mapper.selectReviewTrend(startAt, endExclusive))) {
            reviewedByDate.put(row.statDate(), row);
        }

        Map<LocalDate, BigDecimal> rewardByDate = new LinkedHashMap<>();
        for (AdminDashboardMapper.DateRewardRow row : list(mapper.selectRewardTrend(startAt, endExclusive))) {
            rewardByDate.put(row.statDate(), amount(row.rewardAmount()));
        }

        List<AdminDashboardTrendPoint> trend = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AdminDashboardMapper.DateReviewCountRow reviewRow = reviewedByDate.get(date);
            trend.add(new AdminDashboardTrendPoint(
                    date,
                    submittedByDate.getOrDefault(date, 0L),
                    reviewRow == null ? 0L : value(reviewRow.approvedCount()),
                    reviewRow == null ? 0L : value(reviewRow.rejectedCount()),
                    rewardByDate.getOrDefault(date, BigDecimal.ZERO)
            ));
        }
        return trend;
    }

    private List<AdminDashboardAlert> buildAlerts(LocalDateTime startAt,
                                                  LocalDateTime endExclusive,
                                                  long pendingReviewCount,
                                                  long disabledUserCount) {
        List<AdminDashboardAlert> alerts = new ArrayList<>();
        if (pendingReviewCount >= REVIEW_BACKLOG_THRESHOLD
                || Boolean.TRUE.equals(mapper.existsOverduePendingReview(LocalDateTime.now(clock).minusHours(REVIEW_TIMEOUT_HOURS)))) {
            alerts.add(new AdminDashboardAlert(
                    AdminDashboardAlertType.REVIEW_BACKLOG,
                    AdminDashboardAlertLevel.WARNING,
                    "审核积压",
                    "当前有 " + pendingReviewCount + " 条提交待审核",
                    "/app/reviewer/queue"
            ));
        }

        for (AdminDashboardMapper.HighRejectionTaskRow row : list(mapper.selectHighRejectionRateTasks(startAt, endExclusive))) {
            long approvedCount = value(row.approvedCount());
            long rejectedCount = value(row.rejectedCount());
            long reviewedCount = approvedCount + rejectedCount;
            if (reviewedCount > 0 && rate(rejectedCount, reviewedCount).compareTo(HIGH_REJECTION_RATE_THRESHOLD) >= 0) {
                alerts.add(new AdminDashboardAlert(
                        AdminDashboardAlertType.HIGH_REJECTION_RATE_TASK,
                        AdminDashboardAlertLevel.WARNING,
                        "任务打回率偏高",
                        row.title() + " 当前打回率偏高",
                        "/app/owner/tasks/" + row.taskId()
                ));
            }
        }

        for (AdminDashboardMapper.ZeroSubmissionTaskRow row : list(mapper.selectZeroSubmissionActiveTasks(startAt, endExclusive))) {
            alerts.add(new AdminDashboardAlert(
                    AdminDashboardAlertType.ZERO_SUBMISSION_ACTIVE_TASK,
                    AdminDashboardAlertLevel.INFO,
                    "活跃任务无提交",
                    row.title() + " 已有领取但周期内暂无提交",
                    "/app/owner/tasks/" + row.taskId()
            ));
        }

        if (disabledUserCount > 0) {
            alerts.add(new AdminDashboardAlert(
                    AdminDashboardAlertType.DISABLED_USER,
                    AdminDashboardAlertLevel.INFO,
                    "存在禁用用户",
                    "当前有 " + disabledUserCount + " 个用户被禁用或禁止登录",
                    "/app/admin/users"
            ));
        }
        return alerts;
    }

    private static Map<String, Long> fillCounts(List<String> keys, List<AdminDashboardMapper.KeyCountRow> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : keys) {
            counts.put(key, 0L);
        }
        for (AdminDashboardMapper.KeyCountRow row : list(rows)) {
            if (counts.containsKey(row.name())) {
                counts.put(row.name(), value(row.count()));
            }
        }
        return counts;
    }

    private static BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }
}
