package com.labelhub.modules.submission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
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
import java.util.Collections;
import java.util.function.Function;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final SubmissionUserResolver userResolver;

    public LabelerSubmissionQueryService(SubmissionMapper submissionMapper,
                                         AssignmentMapper assignmentMapper,
                                         AiReviewResultMapper aiReviewResultMapper,
                                         ReviewRecordMapper reviewRecordMapper,
                                         DatasetItemMapper datasetItemMapper,
                                         TemplateVersionMapper templateVersionMapper,
                                         SubmissionUserResolver userResolver) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.userResolver = userResolver;
    }

    public PageResponse<LabelerSubmissionListItem> listSubmissions(Long labelerId,
                                                           Long taskId,
                                                           SubmissionStatus submissionStatus,
                                                           AssignmentStatus assignmentStatus,
                                                           int page, int size) {
        boolean includeAllLabelers = CurrentUserContext.isAdmin();
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.max(1, size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String submissionStatusName = submissionStatus != null ? submissionStatus.name() : null;
        String assignmentStatusName = assignmentStatus != null ? assignmentStatus.name() : null;
        long total = submissionMapper.countLabelerSubmissions(
                labelerId, taskId, submissionStatusName, assignmentStatusName, includeAllLabelers);

        List<Submission> submissions = submissionMapper.selectLabelerSubmissionsPage(
                labelerId, taskId, submissionStatusName, assignmentStatusName,
                includeAllLabelers, normalizedSize, offset);
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<Long> assignmentIds = submissions.stream().map(Submission::getAssignmentId).distinct().toList();

        Map<Long, AiReviewResult> aiResults = submissionIds.isEmpty()
                ? Collections.emptyMap()
                : aiReviewResultMapper.selectBySubmissionIds(submissionIds).stream()
                .collect(Collectors.toMap(AiReviewResult::getSubmissionId,
                        Function.identity(), (first, second) -> first));
        Map<Long, Assignment> assignments = assignmentIds.isEmpty()
                ? Collections.emptyMap()
                : assignmentMapper.selectList(Wrappers.<Assignment>lambdaQuery()
                                .in(Assignment::getId, assignmentIds))
                        .stream()
                        .collect(Collectors.toMap(Assignment::getId,
                                Function.identity(), (first, second) -> first));
        Map<Long, String> rejectReasons = submissionIds.isEmpty()
                ? Collections.emptyMap()
                : reviewRecordMapper.selectLatestRejectBySubmissionIds(submissionIds).stream()
                        .collect(Collectors.toMap(ReviewRecord::getSubmissionId,
                                ReviewRecord::getReason, (first, second) -> first));

        List<LabelerSubmissionListItem> items = submissions.stream().map(s -> {
            AiReviewResult aiResult = aiResults.get(s.getId());
            Assignment assignment = assignments.get(s.getAssignmentId());

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
                    rejectReasons.get(s.getId()),
                    s.getIsGolden(),
                    s.getSubmittedAt(),
                    s.getUpdatedAt()
            );
        }).toList();
        return new PageResponse<>(items, normalizedPage, normalizedSize, total);
    }

    public LabelerSubmissionDetailResponse getDetail(Long submissionId, Long labelerId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "提交记录不存在");
        }
        if (!CurrentUserContext.isAdmin() && !submission.getLabelerId().equals(labelerId)) {
            throw new BusinessException(FORBIDDEN, "当前账号没有权限执行该操作");
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

        Map<Long, String> userNames = userResolver.resolveCreatorNames(versions);

        List<VersionSummary> versionHistory = versions.stream()
                .map(v -> {
                    Long creatorId = userResolver.effectiveCreatorId(v);
                    return new VersionSummary(v.getId(), v.getVersionNo(), v.getStatus(),
                            v.getSubmittedAt(), creatorId,
                            userNames.get(creatorId));
                })
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
