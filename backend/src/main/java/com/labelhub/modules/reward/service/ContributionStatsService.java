package com.labelhub.modules.reward.service;

import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.reward.domain.LabelerContributionStatsEntity;
import com.labelhub.modules.reward.dto.ContributionOverviewResponse;
import com.labelhub.modules.reward.dto.DailyContributionPoint;
import com.labelhub.modules.reward.dto.RewardLedgerResponse;
import com.labelhub.modules.reward.dto.TaskContributionResponse;
import com.labelhub.modules.reward.repository.ContributionStatsMapper;
import com.labelhub.modules.reward.repository.RewardLedgerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 标注员贡献查询服务。该服务只读取 BE-B 统计表和奖励流水，不重新扫描审核状态机。
 */
@Service
public class ContributionStatsService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 31;
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    private final ContributionStatsMapper contributionStatsMapper;
    private final RewardLedgerMapper rewardLedgerMapper;
    private final Clock clock;

    @Autowired
    public ContributionStatsService(ContributionStatsMapper contributionStatsMapper,
                                    RewardLedgerMapper rewardLedgerMapper) {
        this(contributionStatsMapper, rewardLedgerMapper, Clock.systemDefaultZone());
    }

    public ContributionStatsService(ContributionStatsMapper contributionStatsMapper, Clock clock) {
        this(contributionStatsMapper, null, clock);
    }

    public ContributionStatsService(ContributionStatsMapper contributionStatsMapper,
                                    RewardLedgerMapper rewardLedgerMapper,
                                    Clock clock) {
        this.contributionStatsMapper = contributionStatsMapper;
        this.rewardLedgerMapper = rewardLedgerMapper;
        this.clock = clock;
    }

    /**
     * 查询当前标注员总览。通过率只以已审核的通过和拒绝作为分母，待审核不进入分母。
     */
    public ContributionOverviewResponse getOverview() {
        Long labelerId = CurrentUserContext.getUserId();
        LabelerContributionStatsEntity stats = contributionStatsMapper.selectOverviewByLabelerId(labelerId);
        if (stats == null) {
            stats = emptyStats(labelerId);
        }
        int approved = value(stats.getApprovedCount());
        int rejected = value(stats.getRejectedCount());
        return new ContributionOverviewResponse(
                labelerId,
                value(stats.getClaimedCount()),
                value(stats.getSubmittedCount()),
                value(stats.getPendingReviewCount()),
                approved,
                rejected,
                amount(stats.getTotalReward()),
                approvalRate(approved, rejected)
        );
    }

    /**
     * 查询近 N 日趋势，默认 7 日；缺失日期补零，便于前端直接绘图。
     */
    public List<DailyContributionPoint> getTrend(Integer days) {
        Long labelerId = CurrentUserContext.getUserId();
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1L);
        Map<LocalDate, DailyContributionPoint> points = contributionStatsMapper
                .selectDailyTrend(labelerId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(DailyContributionPoint::statDate, Function.identity()));
        return IntStream.range(0, normalizedDays)
                .mapToObj(index -> startDate.plusDays(index))
                .map(date -> points.getOrDefault(date, zeroPoint(date)))
                .toList();
    }

    /**
     * 查询当前标注员按任务汇总的贡献数据。
     */
    public List<TaskContributionResponse> getTasks(Integer limit, Integer offset) {
        return contributionStatsMapper.selectTaskPage(CurrentUserContext.getUserId(),
                normalizeLimit(limit), normalizeOffset(offset));
    }

    /**
     * 查询当前标注员奖励流水。流水包含正向发放和冲正扣回。
     */
    public List<RewardLedgerResponse> getLedger(Integer limit, Integer offset) {
        return rewardLedgerMapper.selectLedgerPage(CurrentUserContext.getUserId(),
                normalizeLimit(limit), normalizeOffset(offset));
    }

    private LabelerContributionStatsEntity emptyStats(Long labelerId) {
        LabelerContributionStatsEntity stats = new LabelerContributionStatsEntity();
        stats.setLabelerId(labelerId);
        stats.setTotalReward(ZERO_AMOUNT);
        return stats;
    }

    private BigDecimal approvalRate(int approved, int rejected) {
        int reviewed = approved + rejected;
        if (reviewed == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(approved).divide(BigDecimal.valueOf(reviewed), 4, RoundingMode.HALF_UP);
    }

    private DailyContributionPoint zeroPoint(LocalDate date) {
        return new DailyContributionPoint(date, 0, 0, 0, ZERO_AMOUNT);
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_TREND_DAYS;
        }
        return Math.max(1, Math.min(days, MAX_TREND_DAYS));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.max(1, Math.min(limit, 100));
    }

    private int normalizeOffset(Integer offset) {
        return Math.max(0, offset == null ? 0 : offset);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO_AMOUNT : value;
    }
}
