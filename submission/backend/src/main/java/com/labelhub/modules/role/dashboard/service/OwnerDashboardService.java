package com.labelhub.modules.role.dashboard.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.role.dashboard.dto.DashboardAlertLevel;
import com.labelhub.modules.role.dashboard.dto.OwnerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.dto.TrendDays;
import com.labelhub.modules.role.dashboard.mapper.OwnerDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OwnerDashboardService {

    private static final int LIST_LIMIT = 5;
    private static final int REVIEW_TIMEOUT_HOURS = 24;
    private static final long REVIEW_BACKLOG_THRESHOLD = 10L;
    private static final long HIGH_REJECTION_MIN_REVIEWED = 5L;
    private static final BigDecimal LOW_CLAIM_RATE_THRESHOLD = new BigDecimal("0.2000");
    private static final BigDecimal HIGH_REJECTION_RATE_THRESHOLD = new BigDecimal("0.5000");
    private static final List<String> TASK_STATUS_KEYS = List.of("DRAFT", "PUBLISHED", "PAUSED", "ENDED");

    private final OwnerDashboardMapper mapper;
    private final Clock clock;

    @Autowired
    public OwnerDashboardService(OwnerDashboardMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    public OwnerDashboardService(OwnerDashboardMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public OwnerDashboardOverviewResponse getOverview(TrendDays trendDays) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        requireExactRole(currentUser, RoleCode.OWNER, "当前账号没有权限查看 Owner 看板");
        Long ownerId = currentUser.userId();

        DateWindow window = DateWindow.of(trendDays.days(), clock);
        OwnerDashboardMapper.ReviewCountRow reviewCounts = mapper.selectReviewCounts(ownerId);
        long approvedCount = reviewCounts == null ? 0L : value(reviewCounts.approvedCount());
        long rejectedCount = reviewCounts == null ? 0L : value(reviewCounts.rejectedCount());
        long reviewedCount = approvedCount + rejectedCount;
        long pendingReviewCount = value(mapper.countPendingReview(ownerId));
        BigDecimal rewardCost = amount(mapper.sumRewardCost(ownerId));

        OwnerDashboardOverviewResponse.OwnerKpis kpis = new OwnerDashboardOverviewResponse.OwnerKpis(
                value(mapper.countTotalTasks(ownerId)),
                value(mapper.countRunningTasks(ownerId)),
                value(mapper.countClaimedItems(ownerId)),
                value(mapper.countSubmittedItems(ownerId)),
                pendingReviewCount,
                rate(approvedCount, reviewedCount),
                rewardCost
        );

        return new OwnerDashboardOverviewResponse(
                trendDays.days(),
                kpis,
                fillCounts(TASK_STATUS_KEYS, mapper.selectTaskStatusDistribution(ownerId)),
                buildTrend(ownerId, window),
                new OwnerDashboardOverviewResponse.QualitySummary(
                        approvedCount,
                        rejectedCount,
                        rate(rejectedCount, reviewedCount)
                ),
                new OwnerDashboardOverviewResponse.RewardSummary(
                        rewardCost,
                        value(mapper.countRewardVisibleTasks(ownerId))
                ),
                buildAttentionTasks(ownerId),
                list(mapper.selectRecentTasks(ownerId, LIST_LIMIT)),
                LocalDateTime.now(clock)
        );
    }

    private List<OwnerDashboardOverviewResponse.DeliveryTrendPoint> buildTrend(Long ownerId, DateWindow window) {
        Map<LocalDate, Long> claimed = dateCounts(mapper.selectClaimedTrend(ownerId, window.startAt(), window.endExclusive()));
        Map<LocalDate, Long> submitted = dateCounts(mapper.selectSubmittedTrend(ownerId, window.startAt(), window.endExclusive()));
        Map<LocalDate, Long> approved = dateCounts(mapper.selectApprovedTrend(ownerId, window.startAt(), window.endExclusive()));

        List<OwnerDashboardOverviewResponse.DeliveryTrendPoint> trend = new ArrayList<>();
        for (LocalDate date = window.startDate(); !date.isAfter(window.endDate()); date = date.plusDays(1)) {
            trend.add(new OwnerDashboardOverviewResponse.DeliveryTrendPoint(
                    date,
                    claimed.getOrDefault(date, 0L),
                    submitted.getOrDefault(date, 0L),
                    approved.getOrDefault(date, 0L)
            ));
        }
        return trend;
    }

    private List<OwnerDashboardOverviewResponse.AttentionTask> buildAttentionTasks(Long ownerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleAt = now.minusHours(REVIEW_TIMEOUT_HOURS);
        List<OwnerDashboardOverviewResponse.AttentionTask> alerts = new ArrayList<>();
        for (OwnerDashboardMapper.AttentionTaskRow row : list(mapper.selectAttentionTaskCandidates(ownerId, staleAt))) {
            long pendingReviewCount = value(row.pendingReviewCount());
            if (pendingReviewCount >= REVIEW_BACKLOG_THRESHOLD) {
                alerts.add(attention(row, "REVIEW_BACKLOG", DashboardAlertLevel.WARNING,
                        "当前有 " + pendingReviewCount + " 条提交待审核"));
                continue;
            }
            long reviewedCount = value(row.approvedCount()) + value(row.rejectedCount());
            if (reviewedCount >= HIGH_REJECTION_MIN_REVIEWED
                    && rate(value(row.rejectedCount()), reviewedCount).compareTo(HIGH_REJECTION_RATE_THRESHOLD) >= 0) {
                alerts.add(attention(row, "HIGH_REJECTION_RATE", DashboardAlertLevel.WARNING,
                        "当前打回率偏高"));
                continue;
            }
            if (row.publishedAt() != null && row.publishedAt().isBefore(staleAt)
                    && value(row.quota()) > 0
                    && rate(value(row.claimedCount()), value(row.quota())).compareTo(LOW_CLAIM_RATE_THRESHOLD) < 0) {
                alerts.add(attention(row, "LOW_CLAIM_RATE", DashboardAlertLevel.INFO,
                        "任务发布超过 24 小时后领取率偏低"));
                continue;
            }
            if (value(row.claimedCount()) > 0
                    && (row.lastSubmittedAt() == null || row.lastSubmittedAt().isBefore(staleAt))) {
                alerts.add(attention(row, "NO_RECENT_SUBMISSION", DashboardAlertLevel.INFO,
                        "已领取任务近 24 小时无新增提交"));
            }
        }
        return alerts.stream()
                .sorted(Comparator.comparing(OwnerDashboardOverviewResponse.AttentionTask::level).reversed())
                .limit(LIST_LIMIT)
                .toList();
    }

    private OwnerDashboardOverviewResponse.AttentionTask attention(OwnerDashboardMapper.AttentionTaskRow row,
                                                                  String type,
                                                                  DashboardAlertLevel level,
                                                                  String description) {
        return new OwnerDashboardOverviewResponse.AttentionTask(
                row.taskId(),
                row.title(),
                type,
                level,
                description,
                "/app/owner/tasks/" + row.taskId() + "/edit"
        );
    }

    static void requireExactRole(CurrentUser currentUser, RoleCode role, String message) {
        if (!currentUser.roles().contains(role)) {
            throw new BusinessException(403001, message);
        }
    }

    private static Map<LocalDate, Long> dateCounts(List<OwnerDashboardMapper.DateCountRow> rows) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (OwnerDashboardMapper.DateCountRow row : list(rows)) {
            counts.put(row.statDate(), value(row.count()));
        }
        return counts;
    }

    static Map<String, Long> fillCounts(List<String> keys, List<OwnerDashboardMapper.KeyCountRow> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : keys) {
            counts.put(key, 0L);
        }
        for (OwnerDashboardMapper.KeyCountRow row : list(rows)) {
            if (counts.containsKey(row.name())) {
                counts.put(row.name(), value(row.count()));
            }
        }
        return counts;
    }

    static BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    static long value(Long value) {
        return value == null ? 0L : value;
    }

    static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    record DateWindow(LocalDate startDate, LocalDate endDate, LocalDateTime startAt, LocalDateTime endExclusive) {
        static DateWindow of(int days, Clock clock) {
            LocalDate endDate = LocalDate.now(clock);
            LocalDate startDate = endDate.minusDays(days - 1L);
            return new DateWindow(
                    startDate,
                    endDate,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay()
            );
        }
    }
}
