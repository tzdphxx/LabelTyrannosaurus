package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewSubmissionMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int SUBMISSION_NOT_FOUND = 404601;
    private static final int SUBMISSION_STATUS_NOT_REVIEWABLE = 400601;
    private static final int REJECT_REASON_REQUIRED = 400602;
    private static final String SUBMISSION_BIZ_TYPE = "SUBMISSION";
    private static final String USER_ACTOR_TYPE = "USER";

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewSubmissionMapper reviewSubmissionMapper;
    private final SubmissionEventPublisher eventPublisher;
    private final AuditAppender auditAppender;

    public ReviewService(SubmissionMapper submissionMapper,
                         AssignmentMapper assignmentMapper,
                         ReviewRecordMapper reviewRecordMapper,
                         ReviewSubmissionMapper reviewSubmissionMapper,
                         SubmissionEventPublisher eventPublisher,
                         AuditAppender auditAppender) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewSubmissionMapper = reviewSubmissionMapper;
        this.eventPublisher = eventPublisher;
        this.auditAppender = auditAppender;
    }

    public List<SubmissionReviewItem> listPendingFinal() {
        return reviewSubmissionMapper.selectPendingFinalItems();
    }

    @Transactional
    public ReviewActionResponse approve(Long submissionId, Long reviewerId, ApproveRequest request) {
        Submission submission = requirePendingFinal(submissionId);

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.APPROVE,
                request.reviewLevel(), null, request.reviewComment());

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setIsGolden(true);
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        assignment.setStatus(AssignmentStatus.APPROVED);
        assignment.setApprovedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        eventPublisher.publishApproved(submissionId, reviewerId);
        appendAudit(submission, reviewerId, "SUBMISSION_APPROVED", record.getId());

        return new ReviewActionResponse(submissionId, SubmissionStatus.APPROVED, record.getId());
    }

    @Transactional
    public ReviewActionResponse reject(Long submissionId, Long reviewerId, RejectRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(REJECT_REASON_REQUIRED, "Reject reason is required");
        }
        Submission submission = requirePendingFinal(submissionId);

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.REJECT,
                request.reviewLevel(), request.reason(), null);

        submission.setStatus(SubmissionStatus.REJECTED);
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        appendAudit(submission, reviewerId, "SUBMISSION_REJECTED", record.getId());

        return new ReviewActionResponse(submissionId, SubmissionStatus.REJECTED, record.getId());
    }

    private Submission requirePendingFinal(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
            throw new BusinessException(SUBMISSION_STATUS_NOT_REVIEWABLE,
                    "Submission is not in PENDING_FINAL status");
        }
        return submission;
    }

    private ReviewRecord createReviewRecord(Long submissionId, Long reviewerId,
                                            ReviewAction action, int reviewLevel,
                                            String reason, String reviewComment) {
        ReviewRecord record = new ReviewRecord();
        record.setSubmissionId(submissionId);
        record.setReviewerId(reviewerId);
        record.setAction(action);
        record.setReviewLevel(reviewLevel);
        record.setReason(reason);
        record.setReviewComment(reviewComment);
        record.setCreatedAt(LocalDateTime.now());
        reviewRecordMapper.insert(record);
        return record;
    }

    private void appendAudit(Submission submission, Long reviewerId,
                              String action, Long reviewRecordId) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("submissionId", submission.getId());
        before.put("status", SubmissionStatus.PENDING_FINAL);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("submissionId", submission.getId());
        after.put("status", submission.getStatus());
        after.put("isGolden", submission.getIsGolden());
        after.put("reviewRecordId", reviewRecordId);

        auditAppender.append(SUBMISSION_BIZ_TYPE, submission.getId(),
                USER_ACTOR_TYPE, reviewerId,
                action, before, after, null, null);
    }
}
