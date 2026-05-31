package com.labelhub.modules.reward;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.modules.dataset.service.DatasetSnapshotService;
import com.labelhub.modules.reward.domain.RewardDirection;
import com.labelhub.modules.reward.domain.RewardLedgerEntity;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import com.labelhub.modules.reward.dto.GoldenSelectedEvent;
import com.labelhub.modules.reward.dto.RewardReversedEvent;
import com.labelhub.modules.reward.dto.SubmissionApprovedEvent;
import com.labelhub.modules.reward.repository.ContributionStatsMapper;
import com.labelhub.modules.reward.repository.RewardLedgerMapper;
import com.labelhub.modules.reward.repository.RewardRuleRepositoryMapper;
import com.labelhub.modules.reward.repository.SubmissionSnapshot;
import com.labelhub.modules.reward.repository.SubmissionSnapshotMapper;
import com.labelhub.modules.reward.service.RewardSettlementService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardSettlementServiceTest {

    private final RewardRuleRepositoryMapper rewardRuleMapper = mock(RewardRuleRepositoryMapper.class);
    private final RewardLedgerMapper rewardLedgerMapper = mock(RewardLedgerMapper.class);
    private final ContributionStatsMapper contributionStatsMapper = mock(ContributionStatsMapper.class);
    private final SubmissionSnapshotMapper submissionSnapshotMapper = mock(SubmissionSnapshotMapper.class);
    private final DatasetSnapshotService datasetSnapshotService = mock(DatasetSnapshotService.class);
    private final AuditAppender auditAppender = mock(AuditAppender.class);
    private final RewardSettlementService rewardSettlementService = new RewardSettlementService(
            rewardRuleMapper,
            rewardLedgerMapper,
            contributionStatsMapper,
            submissionSnapshotMapper,
            datasetSnapshotService,
            auditAppender
    );

    @Test
    void submissionApprovedCreatesCreditLedgerAndStatsOnce() {
        stubRule(new BigDecimal("3.00"));
        when(rewardLedgerMapper.insert(any(RewardLedgerEntity.class))).thenReturn(1);
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "evt-1", 1L, 11L, 100L, 200L, 20L, 30L, LocalDateTime.parse("2026-05-01T10:00:00"), "trace-1");

        rewardSettlementService.settleSubmissionApproved(event);

        ArgumentCaptor<RewardLedgerEntity> captor = ArgumentCaptor.forClass(RewardLedgerEntity.class);
        verify(rewardLedgerMapper).insert(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(RewardDirection.CREDIT);
        assertThat(captor.getValue().getRewardType()).isEqualTo("SUBMISSION_APPROVED");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("3.00");
        verify(contributionStatsMapper).increaseApprovedReward(20L, 1L, new BigDecimal("3.00"),
                LocalDateTime.parse("2026-05-01T10:00:00").toLocalDate());
        verify(datasetSnapshotService).increaseApprovedCount(11L);
    }

    @Test
    void zeroRewardSubmissionApprovedStillUpdatesStatsAndDatasetCount() {
        stubRule(BigDecimal.ZERO);
        when(rewardLedgerMapper.insert(any(RewardLedgerEntity.class))).thenReturn(1);
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "evt-zero", 1L, 11L, 100L, 200L, 20L, 30L, LocalDateTime.parse("2026-05-01T10:00:00"), "trace-1");

        rewardSettlementService.settleSubmissionApproved(event);

        ArgumentCaptor<RewardLedgerEntity> captor = ArgumentCaptor.forClass(RewardLedgerEntity.class);
        verify(rewardLedgerMapper).insert(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("0.00");
        verify(contributionStatsMapper).increaseApprovedReward(20L, 1L, BigDecimal.ZERO,
                LocalDateTime.parse("2026-05-01T10:00:00").toLocalDate());
        verify(datasetSnapshotService).increaseApprovedCount(11L);
    }

    @Test
    void duplicateSubmissionApprovedDoesNotInsertOrUpdateStats() {
        when(rewardLedgerMapper.selectIdBySourceEventId("evt-1")).thenReturn(99L);
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "evt-1", 1L, 11L, 100L, 200L, 20L, 30L, LocalDateTime.parse("2026-05-01T10:00:00"), "trace-1");

        rewardSettlementService.settleSubmissionApproved(event);

        verify(rewardLedgerMapper, never()).insert(any(RewardLedgerEntity.class));
        verify(contributionStatsMapper, never()).increaseApprovedReward(any(), any(), any(), any());
    }

    @Test
    void goldenSelectedSkipsWhenSubmissionAlreadyHasPositiveReward() {
        when(submissionSnapshotMapper.selectRewardSnapshotById(200L))
                .thenReturn(new SubmissionSnapshot(200L, 100L, 1L, 11L, 20L));
        when(rewardLedgerMapper.selectPositiveIdBySubmissionId(200L)).thenReturn(88L);
        GoldenSelectedEvent event = new GoldenSelectedEvent(
                "golden-group-1", 200L, "conflict-1", 30L, LocalDateTime.parse("2026-05-01T11:00:00"), "trace-2");

        rewardSettlementService.settleGoldenSelected(event);

        verify(rewardLedgerMapper, never()).insert(any(RewardLedgerEntity.class));
        verify(contributionStatsMapper, never()).increaseApprovedReward(any(), any(), any(), any());
    }

    @Test
    void rewardReversedAppendsDebitLedgerWithoutDeletingPositiveRow() {
        RewardLedgerEntity positive = new RewardLedgerEntity();
        positive.setId(88L);
        positive.setTaskId(1L);
        positive.setAssignmentId(100L);
        positive.setSubmissionId(200L);
        positive.setLabelerId(20L);
        positive.setAmount(new BigDecimal("3.00"));
        positive.setDirection(RewardDirection.CREDIT);
        positive.setRewardType("SUBMISSION_APPROVED");
        when(rewardLedgerMapper.selectLatestPositiveBySubmissionId(200L)).thenReturn(positive);
        when(rewardLedgerMapper.insert(any(RewardLedgerEntity.class))).thenReturn(1);
        RewardReversedEvent event = new RewardReversedEvent(
                "rev-1", 1L, 200L, 20L, "审核回滚", 30L, LocalDateTime.parse("2026-05-02T12:00:00"), "trace-3");

        rewardSettlementService.reverseReward(event);

        ArgumentCaptor<RewardLedgerEntity> captor = ArgumentCaptor.forClass(RewardLedgerEntity.class);
        verify(rewardLedgerMapper).insert(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(RewardDirection.DEBIT);
        assertThat(captor.getValue().getRewardType()).isEqualTo("REWARD_REVERSED");
        verify(rewardLedgerMapper, never()).deleteById(any(Long.class));
        verify(contributionStatsMapper).decreaseApprovedReward(20L, 1L, new BigDecimal("3.00"),
                LocalDateTime.parse("2026-05-02T12:00:00").toLocalDate());
    }

    @Test
    void rewardReverseIsIdempotentPerSubmission() {
        RewardLedgerEntity positive = new RewardLedgerEntity();
        positive.setId(88L);
        positive.setTaskId(1L);
        positive.setAssignmentId(100L);
        positive.setSubmissionId(200L);
        positive.setLabelerId(20L);
        positive.setAmount(new BigDecimal("3.00"));
        positive.setDirection(RewardDirection.CREDIT);
        positive.setRewardType("SUBMISSION_APPROVED");
        when(rewardLedgerMapper.selectLatestPositiveBySubmissionId(200L)).thenReturn(positive);
        when(rewardLedgerMapper.selectLatestReversedBySubmissionId(200L)).thenReturn(null, positive);
        when(rewardLedgerMapper.insert(any(RewardLedgerEntity.class))).thenReturn(1);

        RewardReversedEvent first = new RewardReversedEvent(
                "rev-1", 1L, 200L, 20L, "瀹℃牳鍥炴粴", 30L, LocalDateTime.parse("2026-05-02T12:00:00"), "trace-3");
        RewardReversedEvent second = new RewardReversedEvent(
                "rev-2", 1L, 200L, 20L, "瀹℃牳鍥炴粴", 30L, LocalDateTime.parse("2026-05-03T12:05:00"), "trace-4");

        rewardSettlementService.reverseReward(first);
        rewardSettlementService.reverseReward(second);

        verify(rewardLedgerMapper, times(1)).insert(any(RewardLedgerEntity.class));
        verify(contributionStatsMapper, times(1)).decreaseApprovedReward(20L, 1L, new BigDecimal("3.00"),
                LocalDateTime.parse("2026-05-02T12:00:00").toLocalDate());
        verify(contributionStatsMapper, never()).decreaseApprovedReward(20L, 1L, new BigDecimal("3.00"),
                LocalDateTime.parse("2026-05-03T12:05:00").toLocalDate());
    }

    private void stubRule(BigDecimal unitReward) {
        RewardRuleEntity rule = new RewardRuleEntity();
        rule.setId(1L);
        rule.setTaskId(1L);
        rule.setEffectiveVersion(1);
        rule.setRewardMode("APPROVED_ITEM");
        rule.setUnitReward(unitReward);
        rule.setRewardCurrency("POINT");
        rule.setRewardVisible(true);
        when(rewardRuleMapper.selectLatestByTaskId(1L)).thenReturn(rule);
    }
}
