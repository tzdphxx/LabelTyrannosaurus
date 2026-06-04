package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.util.AnswerCanonicalizer;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewSubmissionMapper;
import com.labelhub.modules.review.mapper.ReviewTaskMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int SUBMISSION_NOT_FOUND = 404601;
    private static final int SUBMISSION_STATUS_NOT_REVIEWABLE = 400601;
    private static final int REJECT_REASON_REQUIRED = 400602;
    private static final int ASSIGNMENT_NOT_FOUND = 404602;
    private static final int REVIEWER_NOT_ASSIGNED = 403601;
    private static final int INVALID_ANSWER_JSON = 400603;
    private static final String SUBMISSION_BIZ_TYPE = "SUBMISSION";
    private static final String USER_ACTOR_TYPE = "USER";

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewSubmissionMapper reviewSubmissionMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final SubmissionEventPublisher eventPublisher;
    private final AuditAppender auditAppender;
    private final DatasetClaimService datasetClaimService;
    private final ReviewLevelEscalationService escalationService;
    private final ObjectMapper objectMapper;

    public ReviewService(SubmissionMapper submissionMapper,
                         AssignmentMapper assignmentMapper,
                         ReviewRecordMapper reviewRecordMapper,
                         ReviewSubmissionMapper reviewSubmissionMapper,
                         ReviewTaskMapper reviewTaskMapper,
                         SubmissionEventPublisher eventPublisher,
                         AuditAppender auditAppender,
                         DatasetClaimService datasetClaimService,
                         ReviewLevelEscalationService escalationService,
                         ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewSubmissionMapper = reviewSubmissionMapper;
        this.reviewTaskMapper = reviewTaskMapper;
        this.eventPublisher = eventPublisher;
        this.auditAppender = auditAppender;
        this.datasetClaimService = datasetClaimService;
        this.escalationService = escalationService;
        this.objectMapper = objectMapper;
    }

    public List<SubmissionReviewItem> listPendingFinal() {
        return reviewSubmissionMapper.selectPendingFinalItems();
    }

    @Transactional
    public ReviewActionResponse approve(Long submissionId, Long reviewerId, ApproveRequest request) {
        Submission submission = requirePendingFinal(submissionId);
        requireAssignedReviewer(submissionId, reviewerId);
        int currentLevel = request.reviewLevel();
        int maxLevel = escalationService.getMaxReviewLevel(submission.getTaskId());
        requireNotReviewedAtOtherLevel(submissionId, reviewerId, currentLevel);

        if (request.revisedAnswerJson() != null) {
            submission = applyRevision(submission, request.revisedAnswerJson(), reviewerId);
            submissionId = submission.getId();
        }

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.APPROVE,
                currentLevel, null, request.reviewComment());

        if (currentLevel < maxLevel) {
            escalationService.escalate(submission, currentLevel, reviewerId);
            appendAudit(submission, reviewerId, "SUBMISSION_LEVEL_APPROVED", record.getId());
            return new ReviewActionResponse(submissionId, submission.getStatus(), record.getId());
        }

        int affected = submissionMapper.casUpdateStatus(submissionId,
                SubmissionStatus.PENDING_FINAL.name(), SubmissionStatus.APPROVED.name());
        if (affected == 0) {
            throw new BusinessException(SUBMISSION_STATUS_NOT_REVIEWABLE,
                    "Submission was already reviewed by another reviewer");
        }
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setIsGolden(true);
        submission.setReviewFlowStatus("FINAL_APPROVED");
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "关联的领取记录不存在");
        }
        assignment.setStatus(AssignmentStatus.APPROVED);
        assignment.setApprovedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        eventPublisher.publishApproved(submissionId, reviewerId);
        datasetClaimService.increaseApprovedCount(submission.getDatasetItemId());
        appendAudit(submission, reviewerId, "SUBMISSION_APPROVED", record.getId());

        return new ReviewActionResponse(submissionId, SubmissionStatus.APPROVED, record.getId());
    }

    private Submission applyRevision(Submission original, String revisedAnswerJson, Long reviewerId) {
        String canonical;
        try {
            canonical = AnswerCanonicalizer.canonicalize(revisedAnswerJson, objectMapper);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(INVALID_ANSWER_JSON, ex.getMessage());
        }
        String newHash = AnswerCanonicalizer.sha256(canonical);
        if (Objects.equals(original.getAnswerHash(), newHash)) {
            return original;
        }
        submissionMapper.supersedeActiveByAssignmentId(original.getAssignmentId());
        Submission latest = submissionMapper.selectLatestByAssignmentId(original.getAssignmentId());
        int nextVersionNo = latest == null ? 1 : latest.getVersionNo() + 1;

        Submission revised = new Submission();
        revised.setAssignmentId(original.getAssignmentId());
        revised.setTaskId(original.getTaskId());
        revised.setDatasetItemId(original.getDatasetItemId());
        revised.setLabelerId(original.getLabelerId());
        revised.setCreatedBy(reviewerId);
        revised.setTemplateVersionId(original.getTemplateVersionId());
        revised.setVersionNo(nextVersionNo);
        revised.setAnswerJson(canonical);
        revised.setAnswerHash(newHash);
        revised.setStatus(SubmissionStatus.PENDING_FINAL);
        revised.setCurrentReviewLevel(original.getCurrentReviewLevel());
        revised.setReviewFlowStatus(original.getReviewFlowStatus());
        revised.setAssignedReviewerId(original.getAssignedReviewerId());
        revised.setReviewVersion(1);
        submissionMapper.insert(revised);
        return revised;
    }

    @Transactional
    public ReviewActionResponse reject(Long submissionId, Long reviewerId, RejectRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(REJECT_REASON_REQUIRED, "打回原因不能为空");
        }
        Submission submission = requirePendingFinal(submissionId);
        requireAssignedReviewer(submissionId, reviewerId);
        requireNotReviewedAtOtherLevel(submissionId, reviewerId, request.reviewLevel());

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.REJECT,
                request.reviewLevel(), request.reason(), null);

        submission.setStatus(SubmissionStatus.REJECTED);
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "关联的领取记录不存在");
        }
        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        appendAudit(submission, reviewerId, "SUBMISSION_REJECTED", record.getId());

        eventPublisher.publishRejected(submissionId, reviewerId, request.reason());

        return new ReviewActionResponse(submissionId, SubmissionStatus.REJECTED, record.getId());
    }

    private Submission requirePendingFinal(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "提交记录不存在");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
            throw new BusinessException(SUBMISSION_STATUS_NOT_REVIEWABLE,
                    "Submission is not in PENDING_FINAL status");
        }
        return submission;
    }

    private void requireAssignedReviewer(Long submissionId, Long reviewerId) {
        if (CurrentUserContext.isAdmin()) {
            return;
        }
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !reviewerId.equals(submission.getAssignedReviewerId())) {
            throw new BusinessException(REVIEWER_NOT_ASSIGNED,
                    "Reviewer is not assigned to this submission");
        }
    }

    private void requireNotReviewedAtOtherLevel(Long submissionId, Long reviewerId, int currentLevel) {
        int count = reviewRecordMapper.countBySubmissionAndReviewerExcludingLevel(
                submissionId, reviewerId, currentLevel);
        if (count > 0) {
            throw new BusinessException(403601,
                    "Same reviewer cannot review at multiple levels for the same submission");
        }
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

        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, reviewerId,
                SUBMISSION_BIZ_TYPE, submission.getId(),
                action, before, after, null, null));
    }
}
