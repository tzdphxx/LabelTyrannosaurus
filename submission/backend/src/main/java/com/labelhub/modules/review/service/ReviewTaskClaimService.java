package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.review.domain.ReviewTaskClaim;
import com.labelhub.modules.review.dto.ReviewTaskClaimResponse;
import com.labelhub.modules.review.mapper.ReviewTaskClaimMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审核员整任务领取服务。一个 (任务, 审核级别) 只能被一名审核员领取，
 * 领取后该任务该级别下当前及后续进入待审池的提交都归属给该审核员。
 */
@Service
public class ReviewTaskClaimService {

    private static final int TASK_NOT_FOUND = 404601;
    private static final int TASK_NOT_REVIEWABLE = 400601;
    private static final int INVALID_REVIEW_LEVEL = 400603;
    private static final int CLAIM_CONFLICT = 409201;
    private static final int CROSS_LEVEL_CONFLICT = 403601;
    private static final String CLAIM_BIZ_TYPE = "REVIEW_TASK_CLAIM";
    private static final String USER_ACTOR_TYPE = "USER";

    private final ReviewTaskClaimMapper claimMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;

    public ReviewTaskClaimService(ReviewTaskClaimMapper claimMapper,
                                  SubmissionMapper submissionMapper,
                                  TaskMapper taskMapper,
                                  AuditAppender auditAppender,
                                  TraceIdProvider traceIdProvider) {
        this.claimMapper = claimMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
    }

    @Transactional
    public ReviewTaskClaimResponse claim(Long reviewerId, Long taskId, Integer reviewLevel) {
        int level = reviewLevel != null ? reviewLevel : 1;
        Task task = requireReviewableTask(taskId);
        requireValidLevel(task, level);

        ReviewTaskClaim existing = claimMapper.selectByTaskAndLevel(taskId, level);
        if (existing != null) {
            if (reviewerId.equals(existing.getReviewerId())) {
                int count = submissionMapper.assignReviewerForTaskLevel(taskId, level, reviewerId);
                return new ReviewTaskClaimResponse(taskId, level, count);
            }
            throw new BusinessException(CLAIM_CONFLICT,
                    "This task level has already been claimed by another reviewer");
        }

        // 强制多级分离：同一审核员不可同时持有同一任务的不同级别。
        requireNoCrossLevelClaim(taskId, reviewerId);

        ReviewTaskClaim claim = new ReviewTaskClaim();
        claim.setTaskId(taskId);
        claim.setReviewLevel(level);
        claim.setReviewerId(reviewerId);
        claim.setClaimedAt(LocalDateTime.now());
        try {
            claimMapper.insert(claim);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CLAIM_CONFLICT,
                    "This task level was just claimed by another reviewer");
        }

        int claimedCount = submissionMapper.assignReviewerForTaskLevel(taskId, level, reviewerId);
        appendAudit(taskId, level, reviewerId, "REVIEW_TASK_CLAIMED", claimedCount);
        return new ReviewTaskClaimResponse(taskId, level, claimedCount);
    }

    @Transactional
    public void release(Long reviewerId, Long taskId, Integer reviewLevel) {
        int level = reviewLevel != null ? reviewLevel : 1;
        ReviewTaskClaim existing = claimMapper.selectByTaskAndLevel(taskId, level);
        if (existing == null) {
            return;
        }
        if (!reviewerId.equals(existing.getReviewerId())) {
            throw new BusinessException(CLAIM_CONFLICT,
                    "This task level is claimed by another reviewer");
        }
        submissionMapper.clearReviewerForTaskLevel(taskId, level, reviewerId);
        claimMapper.deleteById(existing.getId());
        appendAudit(taskId, level, reviewerId, "REVIEW_TASK_RELEASED", 0);
    }

    private Task requireReviewableTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        if (task.getStatus() == TaskStatus.DRAFT) {
            throw new BusinessException(TASK_NOT_REVIEWABLE,
                    "Draft tasks have no submissions to review");
        }
        return task;
    }

    private void requireValidLevel(Task task, int level) {
        int maxLevel = task.getReviewLevelCount() != null ? task.getReviewLevelCount() : 1;
        if (level < 1 || level > maxLevel) {
            throw new BusinessException(INVALID_REVIEW_LEVEL,
                    "Review level must be between 1 and " + maxLevel);
        }
    }

    private void requireNoCrossLevelClaim(Long taskId, Long reviewerId) {
        for (ReviewTaskClaim c : claimMapper.selectByTask(taskId)) {
            if (reviewerId.equals(c.getReviewerId())) {
                throw new BusinessException(CROSS_LEVEL_CONFLICT,
                        "Same reviewer cannot claim multiple levels of the same task");
            }
        }
    }

    private void appendAudit(Long taskId, int level, Long reviewerId,
                             String action, int claimedCount) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("taskId", taskId);
        after.put("reviewLevel", level);
        after.put("reviewerId", reviewerId);
        after.put("claimedSubmissionCount", claimedCount);
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, reviewerId,
                CLAIM_BIZ_TYPE, taskId, action, null, after, traceIdProvider.currentTraceId(), null));
    }
}
