package com.labelhub.modules.review.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchAssignRequest;
import com.labelhub.modules.review.dto.BatchMarkManualRequest;
import com.labelhub.modules.review.dto.BatchRejectRequest;
import com.labelhub.modules.review.dto.BatchReviewItemResult;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchReviewService {

    private static final int CONFLICT_BATCH_APPROVE_FORBIDDEN = 400603;

    private final ReviewService reviewService;
    private final SubmissionMapper submissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    public BatchReviewService(ReviewService reviewService,
                              SubmissionMapper submissionMapper,
                              ReviewRecordMapper reviewRecordMapper) {
        this.reviewService = reviewService;
        this.submissionMapper = submissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
    }

    @Transactional
    public BatchReviewResponse batchApprove(Long reviewerId, BatchApproveRequest request) {
        List<BatchReviewItemResult> results = new ArrayList<>();
        for (Long submissionId : request.submissionIds()) {
            results.add(trySingleApprove(submissionId, reviewerId, request));
        }
        return buildResponse(results);
    }

    @Transactional
    public BatchReviewResponse batchReject(Long reviewerId, BatchRejectRequest request) {
        List<BatchReviewItemResult> results = new ArrayList<>();
        for (Long submissionId : request.submissionIds()) {
            results.add(trySingleReject(submissionId, reviewerId, request));
        }
        return buildResponse(results);
    }

    @Transactional
    public BatchReviewResponse batchMarkManual(Long reviewerId, BatchMarkManualRequest request) {
        List<BatchReviewItemResult> results = new ArrayList<>();
        for (Long submissionId : request.submissionIds()) {
            results.add(trySingleMarkManual(submissionId, reviewerId));
        }
        return buildResponse(results);
    }

    @Transactional
    public BatchReviewResponse batchAssign(Long reviewerId, BatchAssignRequest request) {
        List<BatchReviewItemResult> results = new ArrayList<>();
        for (Long submissionId : request.submissionIds()) {
            results.add(trySingleAssign(submissionId, reviewerId, request.targetReviewerId()));
        }
        return buildResponse(results);
    }

    private BatchReviewItemResult trySingleApprove(Long submissionId, Long reviewerId,
                                                    BatchApproveRequest request) {
        try {
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                return BatchReviewItemResult.fail(submissionId, "Submission not found");
            }
            if (isConflict(submission)) {
                return BatchReviewItemResult.fail(submissionId,
                        "Conflict submissions cannot be batch approved");
            }
            if (isMarkedManualRequired(submissionId)) {
                return BatchReviewItemResult.fail(submissionId,
                        "Submission is marked for manual review");
            }
            reviewService.approve(submissionId, reviewerId,
                    new ApproveRequest(request.reviewComment(), request.reviewLevel()));
            return BatchReviewItemResult.ok(submissionId);
        } catch (BusinessException ex) {
            return BatchReviewItemResult.fail(submissionId, ex.getMessage());
        }
    }

    private BatchReviewItemResult trySingleReject(Long submissionId, Long reviewerId,
                                                   BatchRejectRequest request) {
        try {
            if (isMarkedManualRequired(submissionId)) {
                return BatchReviewItemResult.fail(submissionId,
                        "Submission is marked for manual review");
            }
            reviewService.reject(submissionId, reviewerId,
                    new RejectRequest(request.reason(), request.reviewLevel()));
            return BatchReviewItemResult.ok(submissionId);
        } catch (BusinessException ex) {
            return BatchReviewItemResult.fail(submissionId, ex.getMessage());
        }
    }

    private BatchReviewItemResult trySingleMarkManual(Long submissionId, Long reviewerId) {
        try {
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                return BatchReviewItemResult.fail(submissionId, "Submission not found");
            }
            if (submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
                return BatchReviewItemResult.fail(submissionId, "Not in PENDING_FINAL status");
            }
            ReviewRecord record = new ReviewRecord();
            record.setSubmissionId(submissionId);
            record.setReviewerId(reviewerId);
            record.setAction(ReviewAction.MARK_MANUAL_REQUIRED);
            record.setReviewLevel(1);
            record.setCreatedAt(java.time.LocalDateTime.now());
            reviewRecordMapper.insert(record);
            return BatchReviewItemResult.ok(submissionId);
        } catch (BusinessException ex) {
            return BatchReviewItemResult.fail(submissionId, ex.getMessage());
        }
    }

    private BatchReviewItemResult trySingleAssign(Long submissionId, Long reviewerId,
                                                   Long targetReviewerId) {
        try {
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                return BatchReviewItemResult.fail(submissionId, "Submission not found");
            }
            if (submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
                return BatchReviewItemResult.fail(submissionId, "Not in PENDING_FINAL status");
            }
            ReviewRecord record = new ReviewRecord();
            record.setSubmissionId(submissionId);
            record.setReviewerId(reviewerId);
            record.setAction(ReviewAction.ASSIGN_REVIEWER);
            record.setReviewLevel(1);
            record.setReason("Assigned to reviewer " + targetReviewerId);
            record.setCreatedAt(java.time.LocalDateTime.now());
            reviewRecordMapper.insert(record);
            return BatchReviewItemResult.ok(submissionId);
        } catch (BusinessException ex) {
            return BatchReviewItemResult.fail(submissionId, ex.getMessage());
        }
    }

    private boolean isConflict(Submission submission) {
        return submissionMapper.countPendingFinalByTaskAndItem(
                submission.getTaskId(), submission.getDatasetItemId()) > 1;
    }

    private boolean isMarkedManualRequired(Long submissionId) {
        LambdaQueryWrapper<ReviewRecord> query = new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getSubmissionId, submissionId)
                .eq(ReviewRecord::getAction, ReviewAction.MARK_MANUAL_REQUIRED);
        return reviewRecordMapper.selectCount(query) > 0;
    }

    private BatchReviewResponse buildResponse(List<BatchReviewItemResult> results) {
        int success = (int) results.stream().filter(BatchReviewItemResult::success).count();
        return new BatchReviewResponse(results.size(), success, results.size() - success, results);
    }
}
