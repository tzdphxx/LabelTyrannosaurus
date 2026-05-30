package com.labelhub.modules.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.AgentRunSummary;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.AiReviewSummary;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.ReviewRecordItem;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.VersionHistoryItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewerSubmissionQueryService {

    private static final int SUBMISSION_NOT_FOUND = 404601;

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final AgentRunMapper agentRunMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    public ReviewerSubmissionQueryService(SubmissionMapper submissionMapper,
                                          AssignmentMapper assignmentMapper,
                                          DatasetItemMapper datasetItemMapper,
                                          TemplateVersionMapper templateVersionMapper,
                                          AiReviewResultMapper aiReviewResultMapper,
                                          AgentRunMapper agentRunMapper,
                                          ReviewRecordMapper reviewRecordMapper) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.agentRunMapper = agentRunMapper;
        this.reviewRecordMapper = reviewRecordMapper;
    }

    public ReviewerSubmissionDetailResponse getDetail(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        DatasetItem item = datasetItemMapper.selectById(submission.getDatasetItemId());
        TemplateVersion tv = templateVersionMapper.selectById(submission.getTemplateVersionId());
        AiReviewResult aiResult = aiReviewResultMapper.selectBySubmissionId(submissionId);

        AgentRunSummary agentRunSummary = null;
        if (aiResult != null && aiResult.getEffectiveRunId() != null) {
            AgentRun run = agentRunMapper.selectById(aiResult.getEffectiveRunId());
            if (run != null) {
                agentRunSummary = new AgentRunSummary(
                        run.getId(), run.getAgentType(), run.getModelName(),
                        run.getStatus().name(), run.getStartedAt(), run.getFinishedAt());
            }
        }

        AiReviewSummary aiSummary = null;
        if (aiResult != null) {
            aiSummary = new AiReviewSummary(
                    aiResult.getId(), aiResult.getEffectiveRunId(),
                    aiResult.getStatus(), aiResult.getDecision(),
                    aiResult.getAverageScore() != null ? aiResult.getAverageScore().toPlainString() : null,
                    aiResult.getRiskFlags(), aiResult.getSuggestion(), aiResult.getErrorCode());
        }

        List<ReviewRecord> records = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getSubmissionId, submissionId)
                        .orderByDesc(ReviewRecord::getCreatedAt));

        List<ReviewRecordItem> reviewRecordItems = records.stream()
                .map(r -> new ReviewRecordItem(r.getId(), r.getReviewerId(),
                        r.getAction().name(), r.getReviewLevel(),
                        r.getReason(), r.getReviewComment(), r.getCreatedAt()))
                .toList();

        List<Submission> versions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, submission.getAssignmentId())
                        .orderByAsc(Submission::getVersionNo));

        List<VersionHistoryItem> versionHistory = versions.stream()
                .map(v -> new VersionHistoryItem(v.getId(), v.getVersionNo(),
                        v.getStatus(), v.getIsGolden(), v.getSubmittedAt()))
                .toList();

        return new ReviewerSubmissionDetailResponse(
                submission.getId(),
                submission.getTaskId(),
                submission.getAssignmentId(),
                submission.getDatasetItemId(),
                submission.getLabelerId(),
                submission.getVersionNo(),
                submission.getStatus(),
                submission.getAnswerJson(),
                item != null ? item.getItemJson() : null,
                submission.getTemplateVersionId(),
                tv != null ? tv.getSchemaJson() : null,
                aiSummary,
                agentRunSummary,
                reviewRecordItems,
                versionHistory
        );
    }
}
