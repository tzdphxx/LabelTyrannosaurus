package com.labelhub.modules.role.dashboard.service;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.role.dashboard.dto.DashboardAlertLevel;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.ReviewerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.mapper.ReviewerDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewerDashboardService {

    private static final int LIST_LIMIT = 5;
    private static final int REVIEW_TIMEOUT_HOURS = 24;

    private final ReviewerDashboardMapper mapper;
    private final Clock clock;

    @Autowired
    public ReviewerDashboardService(ReviewerDashboardMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    public ReviewerDashboardService(ReviewerDashboardMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public ReviewerDashboardOverviewResponse getOverview(DashboardRange range) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        OwnerDashboardService.requireExactRole(currentUser, RoleCode.REVIEWER, "当前账号没有权限查看 Reviewer 看板");
        Long reviewerId = currentUser.userId();

        OwnerDashboardService.DateWindow window = OwnerDashboardService.DateWindow.of(range.days(), clock);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime timeoutAt = now.minusHours(REVIEW_TIMEOUT_HOURS);
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        ReviewerDashboardMapper.ReviewCountRow reviewCounts = mapper.selectTotalReviewCounts(reviewerId);
        long approvedCount = reviewCounts == null ? 0L : OwnerDashboardService.value(reviewCounts.approvedCount());
        long rejectedCount = reviewCounts == null ? 0L : OwnerDashboardService.value(reviewCounts.rejectedCount());
        long reviewedCount = approvedCount + rejectedCount;
        ReviewerDashboardMapper.AiReviewSummaryRow aiSummaryRow = mapper.selectAiReviewSummary(reviewerId);

        return new ReviewerDashboardOverviewResponse(
                range.code(),
                new ReviewerDashboardOverviewResponse.QueueSummary(
                        OwnerDashboardService.value(mapper.countPending(reviewerId)),
                        OwnerDashboardService.value(mapper.countOverdue(reviewerId, now, timeoutAt)),
                        OwnerDashboardService.value(mapper.countManualRequired(reviewerId)),
                        OwnerDashboardService.value(mapper.countConflictRequired(reviewerId))
                ),
                new ReviewerDashboardOverviewResponse.ReviewerKpis(
                        OwnerDashboardService.value(mapper.countTodayReviewed(reviewerId, todayStart, tomorrowStart)),
                        approvedCount,
                        rejectedCount,
                        OwnerDashboardService.rate(approvedCount, reviewedCount),
                        OwnerDashboardService.value(mapper.countAiAttention(reviewerId))
                ),
                buildTrend(reviewerId, window),
                new ReviewerDashboardOverviewResponse.AiReviewSummary(
                        aiSummaryRow == null ? 0L : OwnerDashboardService.value(aiSummaryRow.aiPassCount()),
                        aiSummaryRow == null ? 0L : OwnerDashboardService.value(aiSummaryRow.aiRejectCount()),
                        aiSummaryRow == null ? 0L : OwnerDashboardService.value(aiSummaryRow.manualReviewCount()),
                        aiSummaryRow == null ? 0L : OwnerDashboardService.value(aiSummaryRow.overriddenCount())
                ),
                buildAttentionItems(reviewerId, now, timeoutAt),
                OwnerDashboardService.list(mapper.selectRecentReviewed(reviewerId, LIST_LIMIT)),
                LocalDateTime.now(clock)
        );
    }

    private List<ReviewerDashboardOverviewResponse.ReviewTrendPoint> buildTrend(Long reviewerId,
                                                                                OwnerDashboardService.DateWindow window) {
        Map<LocalDate, ReviewerDashboardMapper.ReviewTrendRow> rows = new LinkedHashMap<>();
        for (ReviewerDashboardMapper.ReviewTrendRow row : OwnerDashboardService.list(
                mapper.selectReviewTrend(reviewerId, window.startAt(), window.endExclusive()))) {
            rows.put(row.statDate(), row);
        }

        List<ReviewerDashboardOverviewResponse.ReviewTrendPoint> trend = new ArrayList<>();
        for (LocalDate date = window.startDate(); !date.isAfter(window.endDate()); date = date.plusDays(1)) {
            ReviewerDashboardMapper.ReviewTrendRow row = rows.get(date);
            trend.add(new ReviewerDashboardOverviewResponse.ReviewTrendPoint(
                    date,
                    row == null ? 0L : OwnerDashboardService.value(row.reviewedCount()),
                    row == null ? 0L : OwnerDashboardService.value(row.approvedCount()),
                    row == null ? 0L : OwnerDashboardService.value(row.rejectedCount())
            ));
        }
        return trend;
    }

    private List<ReviewerDashboardOverviewResponse.AttentionItem> buildAttentionItems(Long reviewerId,
                                                                                      LocalDateTime now,
                                                                                      LocalDateTime timeoutAt) {
        List<ReviewerDashboardOverviewResponse.AttentionItem> items = new ArrayList<>();
        for (ReviewerDashboardMapper.AttentionItemRow row : OwnerDashboardService.list(
                mapper.selectAttentionItems(reviewerId, now, timeoutAt, LIST_LIMIT))) {
            items.add(new ReviewerDashboardOverviewResponse.AttentionItem(
                    row.reviewId(),
                    row.submissionId(),
                    row.taskId(),
                    row.taskTitle(),
                    row.type(),
                    DashboardAlertLevel.valueOf(row.level()),
                    row.description(),
                    row.targetPath()
            ));
        }
        return items;
    }
}
