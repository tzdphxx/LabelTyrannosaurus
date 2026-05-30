package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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

    public AiReviewRecoveryRunner(SubmissionMapper submissionMapper,
                                  AiReviewResultMapper aiReviewResultMapper,
                                  AiReviewDispatcher dispatcher,
                                  AiAutoReviewService aiAutoReviewService,
                                  SystemAgentProvider systemAgentProvider,
                                  AuditAppender auditAppender) {
        this.submissionMapper = submissionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.dispatcher = dispatcher;
        this.aiAutoReviewService = aiAutoReviewService;
        this.systemAgentProvider = systemAgentProvider;
        this.auditAppender = auditAppender;
    }

    @Override
    public void run(ApplicationArguments args) {
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
        if (result.getStatus() == AiReviewStatus.MANUAL_REQUIRED
                || result.getStatus() == AiReviewStatus.SUCCESS) {
            submission.setStatus(SubmissionStatus.PENDING_FINAL);
            submissionMapper.updateById(submission);
            appendRecoveryAudit(submission.getId(), "MOVED_TO_PENDING_FINAL");
        }
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
