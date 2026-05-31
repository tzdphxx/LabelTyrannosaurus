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
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.AgentRunSummary;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.AiReviewSummary;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.LatestPreAnnotationSummary;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.ReviewRecordItem;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse.VersionHistoryItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final PreAnnotationMapper preAnnotationMapper;

    public ReviewerSubmissionQueryService(SubmissionMapper submissionMapper,
                                          AssignmentMapper assignmentMapper,
                                          DatasetItemMapper datasetItemMapper,
                                          TemplateVersionMapper templateVersionMapper,
                                          AiReviewResultMapper aiReviewResultMapper,
                                          AgentRunMapper agentRunMapper,
                                          ReviewRecordMapper reviewRecordMapper,
                                          PreAnnotationMapper preAnnotationMapper) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.agentRunMapper = agentRunMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.preAnnotationMapper = preAnnotationMapper;
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
                    aiResult.getRiskFlags(), aiResult.getSuggestion(), aiResult.getErrorCode(),
                    aiResult.getPromptMode(), aiResult.getDegraded(), aiResult.getLimitations());
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
        LatestPreAnnotationSummary latestPreAnnotation = latestPreAnnotation(submission);

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
                versionHistory,
                latestPreAnnotation
        );
    }

    private LatestPreAnnotationSummary latestPreAnnotation(Submission submission) {
        if (preAnnotationMapper == null) {
            return null;
        }
        PreAnnotation record = preAnnotationMapper.selectLatestByAssignmentId(submission.getAssignmentId());
        if (record == null) {
            return null;
        }
        Map<String, Object> diffMap = new LinkedHashMap<>();
        diffMap.put("suggestedAnswerJson", record.getSuggestedAnswerJson());
        diffMap.put("finalAnswerJson", submission.getAnswerJson());
        String finalDiff = toJson(diffMap);
        return new LatestPreAnnotationSummary(
                record.getId(),
                record.getAgentRunId(),
                record.getStatus() == null ? null : record.getStatus().name(),
                record.getSuggestedAnswerJson(),
                record.getFieldSuggestions(),
                record.getRiskFlags(),
                record.getOverallConfidence() == null ? null : record.getOverallConfidence().toPlainString(),
                record.getLimitations(),
                record.getPromptMode(),
                record.getDegraded(),
                record.getIgnoredFieldsJson(),
                record.getMediaUnderstandingJson(),
                finalDiff
        );
    }

    private String toJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
