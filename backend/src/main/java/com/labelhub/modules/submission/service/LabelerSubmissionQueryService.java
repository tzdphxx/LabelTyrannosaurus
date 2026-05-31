package com.labelhub.modules.submission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.LabelerSubmissionDetailResponse;
import com.labelhub.modules.submission.dto.LabelerSubmissionDetailResponse.ReviewRecordSummary;
import com.labelhub.modules.submission.dto.LabelerSubmissionDetailResponse.VersionSummary;
import com.labelhub.modules.submission.dto.LabelerSubmissionListItem;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LabelerSubmissionQueryService {

    private static final int SUBMISSION_NOT_FOUND = 404501;
    private static final int FORBIDDEN = 403501;
    private static final Set<SubmissionStatus> MODIFIABLE_STATUSES =
            Set.of(SubmissionStatus.REJECTED);

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TemplateVersionMapper templateVersionMapper;

    public LabelerSubmissionQueryService(SubmissionMapper submissionMapper,
                                         AssignmentMapper assignmentMapper,
                                         AiReviewResultMapper aiReviewResultMapper,
                                         ReviewRecordMapper reviewRecordMapper,
                                         DatasetItemMapper datasetItemMapper,
                                         TemplateVersionMapper templateVersionMapper) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
    }

    public List<LabelerSubmissionListItem> listSubmissions(Long labelerId,
                                                           Long taskId,
                                                           SubmissionStatus submissionStatus,
                                                           AssignmentStatus assignmentStatus,
                                                           int page, int size) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getLabelerId, labelerId)
                .ne(Submission::getStatus, SubmissionStatus.SUPERSEDED)
                .eq(taskId != null, Submission::getTaskId, taskId)
                .eq(submissionStatus != null, Submission::getStatus, submissionStatus)
                .orderByDesc(Submission::getSubmittedAt)
                .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));

        List<Submission> submissions = submissionMapper.selectList(wrapper);

        return submissions.stream().map(s -> {
            AiReviewResult aiResult = aiReviewResultMapper.selectBySubmissionId(s.getId());
            Assignment assignment = assignmentMapper.selectById(s.getAssignmentId());

            if (assignmentStatus != null && assignment != null
                    && assignment.getStatus() != assignmentStatus) {
                return null;
            }

            String rejectReason = null;
            if (s.getStatus() == SubmissionStatus.REJECTED) {
                ReviewRecord rr = reviewRecordMapper.selectOne(
                        new LambdaQueryWrapper<ReviewRecord>()
                                .eq(ReviewRecord::getSubmissionId, s.getId())
                                .eq(ReviewRecord::getAction, ReviewAction.REJECT)
                                .orderByDesc(ReviewRecord::getCreatedAt)
                                .last("LIMIT 1"));
                if (rr != null) rejectReason = rr.getReason();
            }

            return new LabelerSubmissionListItem(
                    s.getId(),
                    s.getAssignmentId(),
                    s.getTaskId(),
                    s.getDatasetItemId(),
                    s.getVersionNo(),
                    s.getStatus(),
                    assignment != null ? assignment.getStatus() : null,
                    aiResult != null ? aiResult.getStatus() : null,
                    aiResult != null ? aiResult.getDecision() : null,
                    null,
                    rejectReason,
                    s.getIsGolden(),
                    s.getSubmittedAt(),
                    s.getUpdatedAt()
            );
        }).filter(item -> item != null).toList();
    }

    public LabelerSubmissionDetailResponse getDetail(Long submissionId, Long labelerId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }
        if (!submission.getLabelerId().equals(labelerId)) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        DatasetItem item = datasetItemMapper.selectById(submission.getDatasetItemId());
        TemplateVersion tv = templateVersionMapper.selectById(submission.getTemplateVersionId());
        AiReviewResult aiResult = aiReviewResultMapper.selectBySubmissionId(submissionId);

        List<ReviewRecord> records = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getSubmissionId, submissionId)
                        .orderByDesc(ReviewRecord::getCreatedAt));

        String rejectReason = records.stream()
                .filter(r -> r.getAction() == ReviewAction.REJECT)
                .findFirst()
                .map(ReviewRecord::getReason)
                .orElse(null);

        List<ReviewRecordSummary> reviewSummaries = records.stream()
                .map(r -> new ReviewRecordSummary(
                        r.getId(), r.getAction().name(), r.getReason(), r.getCreatedAt()))
                .toList();

        List<Submission> versions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, submission.getAssignmentId())
                        .orderByAsc(Submission::getVersionNo));

        List<VersionSummary> versionHistory = versions.stream()
                .map(v -> new VersionSummary(v.getId(), v.getVersionNo(), v.getStatus(), v.getSubmittedAt()))
                .toList();

        boolean canModify = assignment != null
                && assignment.getStatus() == AssignmentStatus.RETURNED;

        return new LabelerSubmissionDetailResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getTaskId(),
                submission.getDatasetItemId(),
                submission.getTemplateVersionId(),
                submission.getVersionNo(),
                submission.getStatus(),
                assignment != null ? assignment.getStatus() : null,
                item != null ? item.getItemJson() : null,
                tv != null ? tv.getSchemaJson() : null,
                submission.getAnswerJson(),
                aiResult != null ? aiResult.getStatus() : null,
                aiResult != null ? aiResult.getDecision() : null,
                aiResult != null ? aiResult.getSuggestion() : null,
                rejectReason,
                reviewSummaries,
                versionHistory,
                canModify
        );
    }
}
