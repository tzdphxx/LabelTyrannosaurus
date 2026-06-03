package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ReviewClaimResponse;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReviewClaimService {

    private static final Logger log = LoggerFactory.getLogger(ReviewClaimService.class);
    private static final int CLAIM_CONFLICT = 409201;
    private static final int MAX_CLAIM_COUNT = 50;
    private static final long CLAIM_LOCK_WAIT_MILLIS = 3000L;
    private static final long CLAIM_LOCK_LEASE_MILLIS = 15000L;
    private static final String SUBMISSION_BIZ_TYPE = "SUBMISSION";
    private static final String USER_ACTOR_TYPE = "USER";

    private final SubmissionMapper submissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final RedisLockService redisLockService;
    private final AuditAppender auditAppender;
    private final TransactionTemplate transactionTemplate;

    public ReviewClaimService(SubmissionMapper submissionMapper,
                              ReviewRecordMapper reviewRecordMapper,
                              RedisLockService redisLockService,
                              AuditAppender auditAppender,
                              TransactionTemplate transactionTemplate) {
        this.submissionMapper = submissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.redisLockService = redisLockService;
        this.auditAppender = auditAppender;
        this.transactionTemplate = transactionTemplate;
    }

    public ReviewClaimResponse claim(Long reviewerId, int count, Long taskId) {
        int safeCount = Math.max(1, Math.min(count, MAX_CLAIM_COUNT));
        String lockKey = "lock:review-claim:reviewer:" + reviewerId;
        boolean locked = redisLockService.tryLock(lockKey, CLAIM_LOCK_WAIT_MILLIS, CLAIM_LOCK_LEASE_MILLIS);
        if (!locked) {
            throw new BusinessException(CLAIM_CONFLICT, "审核领取繁忙，请稍后重试");
        }
        try {
            List<Long> claimed = transactionTemplate.execute(status -> doClaim(reviewerId, safeCount, taskId));
            return new ReviewClaimResponse(claimed != null ? claimed : List.of(),
                    claimed != null ? claimed.size() : 0);
        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    private List<Long> doClaim(Long reviewerId, int count, Long taskId) {
        List<Long> candidates = (taskId != null)
                ? submissionMapper.selectUnassignedPendingFinalByTask(taskId, count)
                : submissionMapper.selectUnassignedPendingFinal(count);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Long> claimed = new ArrayList<>();
        for (Long submissionId : candidates) {
            int affected = submissionMapper.assignReviewer(submissionId, reviewerId);
            if (affected > 0) {
                writeReviewRecord(submissionId, reviewerId);
                appendClaimAudit(submissionId, reviewerId);
                claimed.add(submissionId);
            }
        }
        log.info("Reviewer {} claimed {} submissions (requested {})", reviewerId, claimed.size(), count);
        return claimed;
    }

    private void writeReviewRecord(Long submissionId, Long reviewerId) {
        ReviewRecord record = new ReviewRecord();
        record.setSubmissionId(submissionId);
        record.setReviewerId(reviewerId);
        record.setAction(ReviewAction.ASSIGN_REVIEWER);
        record.setReviewLevel(1);
        record.setReason("Self-claimed by reviewer");
        record.setCreatedAt(LocalDateTime.now());
        reviewRecordMapper.insert(record);
    }

    private void appendClaimAudit(Long submissionId, Long reviewerId) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("submissionId", submissionId);
        after.put("assignedReviewerId", reviewerId);
        after.put("claimType", "SELF_CLAIM");
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, reviewerId,
                SUBMISSION_BIZ_TYPE, submissionId,
                "REVIEW_CLAIMED", null, after, null, null));
    }
}