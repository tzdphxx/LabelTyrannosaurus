package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.domain.ConflictGroup;
import com.labelhub.modules.review.domain.ConflictStatus;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ConflictGroupResponse;
import com.labelhub.modules.review.dto.ConflictResolveRequest;
import com.labelhub.modules.review.dto.ConflictResolveResponse;
import com.labelhub.modules.review.mapper.ConflictGroupMapper;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConflictResolveService {

    private static final int GROUP_NOT_FOUND = 404701;
    private static final int GROUP_ALREADY_RESOLVED = 400701;
    private static final int SUBMISSION_NOT_IN_GROUP = 400702;
    private static final int SUBMISSION_NOT_REVIEWABLE = 400703;
    private static final String SUBMISSION_BIZ_TYPE = "SUBMISSION";
    private static final String USER_ACTOR_TYPE = "USER";

    private final ConflictGroupMapper conflictGroupMapper;
    private final SubmissionMapper submissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final SubmissionEventPublisher eventPublisher;
    private final AuditAppender auditAppender;

    public ConflictResolveService(ConflictGroupMapper conflictGroupMapper,
                                   SubmissionMapper submissionMapper,
                                   ReviewRecordMapper reviewRecordMapper,
                                   SubmissionEventPublisher eventPublisher,
                                   AuditAppender auditAppender) {
        this.conflictGroupMapper = conflictGroupMapper;
        this.submissionMapper = submissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.eventPublisher = eventPublisher;
        this.auditAppender = auditAppender;
    }

    public List<ConflictGroupResponse> listOpenGroups() {
        return conflictGroupMapper.selectOpenGroups().stream()
                .map(this::toResponse)
                .toList();
    }

    public ConflictGroupResponse getGroup(Long groupId) {
        ConflictGroup group = conflictGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(GROUP_NOT_FOUND, "Conflict group not found");
        }
        return toResponse(group);
    }

    @Transactional
    public ConflictResolveResponse resolve(Long groupId, Long reviewerId, ConflictResolveRequest request) {
        ConflictGroup group = conflictGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(GROUP_NOT_FOUND, "Conflict group not found");
        }
        if (group.getStatus() == ConflictStatus.RESOLVED) {
            throw new BusinessException(GROUP_ALREADY_RESOLVED, "Conflict group already resolved");
        }

        Submission golden = submissionMapper.selectById(request.goldenSubmissionId());
        if (golden == null
                || !golden.getTaskId().equals(group.getTaskId())
                || !golden.getDatasetItemId().equals(group.getDatasetItemId())) {
            throw new BusinessException(SUBMISSION_NOT_IN_GROUP,
                    "Submission does not belong to this conflict group");
        }
        if (golden.getStatus() != SubmissionStatus.PENDING_FINAL) {
            throw new BusinessException(SUBMISSION_NOT_REVIEWABLE,
                    "Submission is not in reviewable status");
        }

        golden.setIsGolden(true);
        golden.setStatus(SubmissionStatus.APPROVED);
        submissionMapper.updateById(golden);

        List<Submission> siblings = submissionMapper.selectPendingFinalByTaskAndItem(
                group.getTaskId(), group.getDatasetItemId());
        for (Submission s : siblings) {
            if (!s.getId().equals(golden.getId())) {
                s.setStatus(SubmissionStatus.REJECTED);
                s.setIsGolden(false);
                submissionMapper.updateById(s);
            }
        }

        group.setStatus(ConflictStatus.RESOLVED);
        group.setGoldenSubmissionId(request.goldenSubmissionId());
        group.setResolvedBy(reviewerId);
        group.setResolvedReason(request.reason());
        group.setResolvedAt(LocalDateTime.now());
        conflictGroupMapper.updateById(group);

        ReviewRecord record = new ReviewRecord();
        record.setSubmissionId(request.goldenSubmissionId());
        record.setReviewerId(reviewerId);
        record.setAction(ReviewAction.RESOLVE_CONFLICT);
        record.setReviewLevel(1);
        record.setReason(request.reason());
        record.setCreatedAt(LocalDateTime.now());
        reviewRecordMapper.insert(record);

        eventPublisher.publishGoldenSelected(request.goldenSubmissionId(), reviewerId);
        appendAudit(group, golden, reviewerId, record.getId());

        return new ConflictResolveResponse(groupId, ConflictStatus.RESOLVED,
                request.goldenSubmissionId(), record.getId());
    }

    @Transactional
    public void detectAndCreateConflict(Long taskId, Long datasetItemId) {
        List<Submission> pendingFinals = submissionMapper.selectPendingFinalByTaskAndItem(taskId, datasetItemId);
        if (pendingFinals.size() < 2) {
            return;
        }

        Map<String, Long> hashCounts = pendingFinals.stream()
                .collect(Collectors.groupingBy(Submission::getAnswerHash, Collectors.counting()));
        long maxSame = hashCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        BigDecimal score = BigDecimal.valueOf(maxSame)
                .divide(BigDecimal.valueOf(pendingFinals.size()), 4, RoundingMode.HALF_UP);

        if (score.compareTo(BigDecimal.ONE) >= 0) {
            return;
        }

        ConflictGroup existing = conflictGroupMapper.selectByTaskAndItem(taskId, datasetItemId);
        if (existing != null) {
            existing.setConsensusScore(score);
            conflictGroupMapper.updateById(existing);
            return;
        }

        ConflictGroup group = new ConflictGroup();
        group.setTaskId(taskId);
        group.setDatasetItemId(datasetItemId);
        group.setStatus(ConflictStatus.OPEN);
        group.setConsensusScore(score);
        group.setCreatedAt(LocalDateTime.now());
        conflictGroupMapper.insert(group);
    }

    private void appendAudit(ConflictGroup group, Submission golden,
                              Long reviewerId, Long reviewRecordId) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("conflictGroupId", group.getId());
        before.put("status", ConflictStatus.OPEN);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("conflictGroupId", group.getId());
        after.put("status", ConflictStatus.RESOLVED);
        after.put("goldenSubmissionId", golden.getId());
        after.put("reviewRecordId", reviewRecordId);

        auditAppender.append(SUBMISSION_BIZ_TYPE, golden.getId(),
                USER_ACTOR_TYPE, reviewerId,
                "CONFLICT_RESOLVED", before, after, null, null);
    }

    private ConflictGroupResponse toResponse(ConflictGroup group) {
        return new ConflictGroupResponse(
                group.getId(), group.getTaskId(), group.getDatasetItemId(),
                group.getStatus(), group.getConsensusScore(),
                group.getGoldenSubmissionId(), group.getCreatedAt(), group.getResolvedAt());
    }
}