package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiReviewRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiReviewRecoveryRunner.class);
    private static final String BIZ_TYPE = "AI_REVIEW";
    private static final Set<AiReviewStatus> TERMINAL_STATUSES = Set.of(
            AiReviewStatus.SUCCESS, AiReviewStatus.MANUAL_REQUIRED);

    private final SubmissionMapper submissionMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final AiReviewDispatcher dispatcher;
    private final AiAutoReviewService aiAutoReviewService;
    private final SystemAgentProvider systemAgentProvider;
    private final AuditAppender auditAppender;
    private final com.labelhub.modules.review.service.ReviewOwnershipResolver reviewOwnershipResolver;
    private final AiReviewSchemaReadiness schemaReadiness;
    private final RedisLockService redisLockService;

    public AiReviewRecoveryRunner(SubmissionMapper submissionMapper,
                                  AiReviewResultMapper aiReviewResultMapper,
                                  AiReviewDispatcher dispatcher,
                                  AiAutoReviewService aiAutoReviewService,
                                  SystemAgentProvider systemAgentProvider,
                                  AuditAppender auditAppender,
                                  com.labelhub.modules.review.service.ReviewOwnershipResolver reviewOwnershipResolver,
                                  AiReviewSchemaReadiness schemaReadiness,
                                  RedisLockService redisLockService) {
        this.submissionMapper = submissionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.dispatcher = dispatcher;
        this.aiAutoReviewService = aiAutoReviewService;
        this.systemAgentProvider = systemAgentProvider;
        this.auditAppender = auditAppender;
        this.reviewOwnershipResolver = reviewOwnershipResolver;
        this.schemaReadiness = schemaReadiness;
        this.redisLockService = redisLockService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!schemaReadiness.isReady()) {
            log.warn("Skipping AI review recovery because required database tables are not ready");
            return;
        }
        List<Submission> stuck = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStatus, SubmissionStatus.AI_REVIEWING));
        if (stuck.isEmpty()) {
            return;
        }
        log.info("AI review recovery: found {} stuck submissions", stuck.size());

        for (Submission submission : stuck) {
            try {
                recover(submission);
            } catch (Exception e) {
                log.error("Recovery failed for submission {}", submission.getId(), e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${labelhub.ai.review-recovery-delay-ms:60000}")
    public void recoverMissingResultsPeriodically() {
        if (!schemaReadiness.isReady()) {
            log.warn("Skipping periodic AI review recovery because required database tables are not ready");
            return;
        }
        List<Submission> candidates = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStatus, SubmissionStatus.AI_REVIEWING));
        if (candidates.isEmpty()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);
        for (Submission submission : candidates) {
            if (!isOldEnoughForMissingResultRecovery(submission, cutoff)) {
                continue;
            }
            try {
                redisLockService.withLock(RedisKeyBuilder.aiReviewLock(submission.getId()),
                        1000L, 300000L, () -> recoverMissingResult(submission.getId(), cutoff));
            } catch (Exception e) {
                log.warn("Periodic AI review recovery failed for submission {}", submission.getId(), e);
            }
        }
    }

    private boolean isOldEnoughForMissingResultRecovery(Submission submission, LocalDateTime cutoff) {
        return submission.getSubmittedAt() == null || submission.getSubmittedAt().isBefore(cutoff);
    }

    private void recoverMissingResult(Long submissionId, LocalDateTime cutoff) {
        Submission latest = submissionMapper.selectById(submissionId);
        if (latest == null || latest.getStatus() != SubmissionStatus.AI_REVIEWING
                || !isOldEnoughForMissingResultRecovery(latest, cutoff)) {
            return;
        }
        AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (existing != null) {
            return;
        }
        log.info("Re-queuing AI review missing result for submission {}", submissionId);
        dispatcher.enqueue(submissionId);
        appendRecoveryAudit(submissionId, "RE_QUEUED_MISSING_RESULT");
    }

    private void recover(Submission submission) {
        AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submission.getId());
        if (existing != null && TERMINAL_STATUSES.contains(existing.getStatus())) {
            moveToFinalStatus(submission, existing);
            return;
        }

        if (existing != null) {
            log.info("Retrying failed AI review for submission {}", submission.getId());
            aiAutoReviewService.retryReview(submission.getId());
            appendRecoveryAudit(submission.getId(), "RETRIED");
        } else {
            log.info("Re-queuing AI review for submission {}", submission.getId());
            dispatcher.enqueue(submission.getId());
            appendRecoveryAudit(submission.getId(), "RE_QUEUED");
        }
    }

    private void moveToFinalStatus(Submission submission, AiReviewResult result) {
        if (result.getStatus() == AiReviewStatus.MANUAL_REQUIRED) {
            updateRecoveredStatus(submission, SubmissionStatus.PENDING_FINAL, "MOVED_TO_PENDING_FINAL");
            return;
        }
        if (result.getStatus() != AiReviewStatus.SUCCESS) {
            return;
        }
        try {
            aiAutoReviewService.applyRecoveredFlowAction(submission, result);
        } catch (Exception e) {
            log.error("Failed to apply recovered flow action for submission {} (action={}) — "
                    + "falling back to PENDING_FINAL",
                    submission.getId(), result.getFlowAction(), e);
            updateRecoveredStatus(submission, SubmissionStatus.PENDING_FINAL,
                    "FALLBACK_TO_PENDING_FINAL");
            return;
        }
        appendRecoveryAudit(submission.getId(), "RECOVERED_" + result.getFlowAction());
    }

    private void updateRecoveredStatus(Submission submission, SubmissionStatus status, String action) {
        submission.setStatus(status);
        submissionMapper.updateById(submission);
        if (status == SubmissionStatus.PENDING_FINAL) {
            reviewOwnershipResolver.assignToClaimant(submission);
        }
        appendRecoveryAudit(submission.getId(), action);
    }

    private void appendRecoveryAudit(Long submissionId, String action) {
        SystemActorContext actor = systemAgentProvider.get();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", submissionId);
        snapshot.put("recoveryAction", action);
        auditAppender.append(new AuditCommand(SystemActorContext.ACTOR_TYPE, actor.agentId(),
                BIZ_TYPE, submissionId,
                "AI_REVIEW_RECOVERY", null, snapshot, "startup-recovery", null));
    }
}
