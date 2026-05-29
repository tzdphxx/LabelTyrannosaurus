package com.labelhub.modules.reward.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.dataset.service.DatasetSnapshotService;
import com.labelhub.modules.reward.domain.RewardDirection;
import com.labelhub.modules.reward.domain.RewardLedgerEntity;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import com.labelhub.modules.reward.dto.GoldenSelectedEvent;
import com.labelhub.modules.reward.dto.RewardReversedEvent;
import com.labelhub.modules.reward.dto.SubmissionApprovedEvent;
import com.labelhub.modules.reward.repository.ContributionStatsMapper;
import com.labelhub.modules.reward.repository.RewardLedgerMapper;
import com.labelhub.modules.reward.repository.RewardRuleMapper;
import com.labelhub.modules.reward.repository.SubmissionSnapshot;
import com.labelhub.modules.reward.repository.SubmissionSnapshotMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 奖励事件结算服务。BE-B 消费 BE-A 事件快照，只写奖励和贡献统计，不推进审核状态机。
 */
@Service
public class RewardSettlementService {

    private static final String TYPE_SUBMISSION_APPROVED = "SUBMISSION_APPROVED";
    private static final String TYPE_GOLDEN_SELECTED = "GOLDEN_SELECTED";
    private static final String TYPE_REWARD_REVERSED = "REWARD_REVERSED";

    private final RewardRuleMapper rewardRuleMapper;
    private final RewardLedgerMapper rewardLedgerMapper;
    private final ContributionStatsMapper contributionStatsMapper;
    private final SubmissionSnapshotMapper submissionSnapshotMapper;
    private final DatasetSnapshotService datasetSnapshotService;
    private final AuditAppender auditAppender;

    public RewardSettlementService(RewardRuleMapper rewardRuleMapper,
                                   RewardLedgerMapper rewardLedgerMapper,
                                   ContributionStatsMapper contributionStatsMapper,
                                   SubmissionSnapshotMapper submissionSnapshotMapper,
                                   DatasetSnapshotService datasetSnapshotService,
                                   AuditAppender auditAppender) {
        this.rewardRuleMapper = rewardRuleMapper;
        this.rewardLedgerMapper = rewardLedgerMapper;
        this.contributionStatsMapper = contributionStatsMapper;
        this.submissionSnapshotMapper = submissionSnapshotMapper;
        this.datasetSnapshotService = datasetSnapshotService;
        this.auditAppender = auditAppender;
    }

    /**
     * 结算审核通过事件。幂等依赖 source_event_id、positive_submission_id 和 positive_assignment_id 唯一约束兜底。
     */
    @Transactional
    public void settleSubmissionApproved(SubmissionApprovedEvent event) {
        if (alreadyConsumed(event.eventId())
                || alreadyRewarded(event.submissionId(), event.assignmentId())) {
            return;
        }
        RewardRuleEntity rule = requireLatestRule(event.taskId());
        RewardLedgerEntity ledger = creditLedger(event.taskId(), event.labelerId(), event.submissionId(),
                event.assignmentId(), rule.getUnitReward(), "审核通过奖励", event.eventId(), TYPE_SUBMISSION_APPROVED);
        if (insertCreditAndStats(ledger, event.datasetItemId(), event.approvedAt().toLocalDate())) {
            appendAudit("REWARD_SETTLED", event.reviewerId(), ledger, event.traceId());
        }
    }

    /**
     * 结算金标选中事件。BE-B 只读 submissions 快照补齐 assignmentId，不修改 BE-A 状态字段。
     */
    @Transactional
    public void settleGoldenSelected(GoldenSelectedEvent event) {
        if (alreadyConsumed(event.eventId())) {
            return;
        }
        SubmissionSnapshot snapshot = requireSubmissionSnapshot(event.goldenSubmissionId());
        if (alreadyRewarded(snapshot.submissionId(), snapshot.assignmentId())) {
            return;
        }
        RewardRuleEntity rule = requireLatestRule(snapshot.taskId());
        RewardLedgerEntity ledger = creditLedger(snapshot.taskId(), snapshot.labelerId(), snapshot.submissionId(),
                snapshot.assignmentId(), rule.getUnitReward(), "金标选中奖励", event.eventId(), TYPE_GOLDEN_SELECTED);
        if (insertCreditAndStats(ledger, snapshot.datasetItemId(), event.resolvedAt().toLocalDate())) {
            appendAudit("GOLDEN_REWARD_SETTLED", event.reviewerId(), ledger, event.traceId());
        }
    }

    /**
     * 冲正已发奖励。原正向流水保持不变，新的 DEBIT 流水用于表达扣回。
     */
    @Transactional
    public void reverseReward(RewardReversedEvent event) {
        if (alreadyConsumed(event.eventId())) {
            return;
        }
        RewardLedgerEntity positive = rewardLedgerMapper.selectLatestPositiveBySubmissionId(event.submissionId());
        if (positive == null) {
            throw new BusinessException(400102, "Positive reward not found");
        }
        if (alreadyReversed(event.submissionId())) {
            return;
        }
        RewardLedgerEntity debit = new RewardLedgerEntity();
        debit.setTaskId(positive.getTaskId());
        debit.setLabelerId(positive.getLabelerId());
        debit.setSubmissionId(positive.getSubmissionId());
        debit.setAssignmentId(positive.getAssignmentId());
        debit.setAmount(positive.getAmount());
        debit.setDirection(RewardDirection.DEBIT);
        debit.setReason(event.reason());
        debit.setSourceEventId(event.eventId());
        debit.setRewardType(TYPE_REWARD_REVERSED);

        // 冲正幂等依赖 reward_ledger.source_event_id 唯一约束兜底；这里不删除任何正向流水。
        try {
            rewardLedgerMapper.insert(debit);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        contributionStatsMapper.decreaseApprovedReward(positive.getLabelerId(), positive.getTaskId(),
                positive.getAmount(), event.createdAt().toLocalDate());
        appendAudit("REWARD_REVERSED", event.operatorId(), debit, event.traceId());
    }

    private boolean insertCreditAndStats(RewardLedgerEntity ledger, Long datasetItemId, LocalDate statDate) {
        if (ledger.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        // 最终幂等依赖 reward_ledger.source_event_id、positive_submission_id、positive_assignment_id 唯一约束。
        try {
            rewardLedgerMapper.insert(ledger);
        } catch (DuplicateKeyException ignored) {
            return false;
        }
        contributionStatsMapper.increaseApprovedReward(ledger.getLabelerId(), ledger.getTaskId(),
                ledger.getAmount(), statDate);
        datasetSnapshotService.increaseApprovedCount(datasetItemId);
        return true;
    }

    private boolean alreadyReversed(Long submissionId) {
        return rewardLedgerMapper.selectLatestReversedBySubmissionId(submissionId) != null;
    }

    private boolean alreadyConsumed(String eventId) {
        return positiveId(rewardLedgerMapper.selectIdBySourceEventId(eventId));
    }

    private boolean alreadyRewarded(Long submissionId, Long assignmentId) {
        return positiveId(rewardLedgerMapper.selectPositiveIdBySubmissionId(submissionId))
                || positiveId(rewardLedgerMapper.selectPositiveIdByAssignmentId(assignmentId));
    }

    private boolean positiveId(Long id) {
        return id != null && id > 0;
    }

    private RewardRuleEntity requireLatestRule(Long taskId) {
        RewardRuleEntity rule = rewardRuleMapper.selectLatestByTaskId(taskId);
        if (rule == null) {
            throw new BusinessException(400102, "Reward rule not found");
        }
        return rule;
    }

    private SubmissionSnapshot requireSubmissionSnapshot(Long submissionId) {
        SubmissionSnapshot snapshot = submissionSnapshotMapper.selectRewardSnapshotById(submissionId);
        if (snapshot == null) {
            throw new BusinessException(400102, "Submission not found");
        }
        return snapshot;
    }

    private RewardLedgerEntity creditLedger(Long taskId,
                                            Long labelerId,
                                            Long submissionId,
                                            Long assignmentId,
                                            BigDecimal amount,
                                            String reason,
                                            String sourceEventId,
                                            String rewardType) {
        RewardLedgerEntity ledger = new RewardLedgerEntity();
        ledger.setTaskId(taskId);
        ledger.setLabelerId(labelerId);
        ledger.setSubmissionId(submissionId);
        ledger.setAssignmentId(assignmentId);
        ledger.setAmount(amount);
        ledger.setDirection(RewardDirection.CREDIT);
        ledger.setReason(reason);
        ledger.setSourceEventId(sourceEventId);
        ledger.setRewardType(rewardType);
        return ledger;
    }

    private void appendAudit(String action, Long actorId, RewardLedgerEntity ledger, String traceId) {
        auditAppender.append(new AuditCommand(
                "USER",
                actorId,
                "REWARD_LEDGER",
                ledger.getId(),
                action,
                Map.of(),
                Map.of(
                        "taskId", ledger.getTaskId(),
                        "labelerId", ledger.getLabelerId(),
                        "submissionId", ledger.getSubmissionId(),
                        "amount", ledger.getAmount(),
                        "direction", ledger.getDirection().name()
                ),
                traceId,
                null
        ));
    }
}
