package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewerSubmissionDetailResponse(
        Long submissionId,
        Long taskId,
        Long assignmentId,
        Long datasetItemId,
        Long labelerId,
        Integer versionNo,
        SubmissionStatus submissionStatus,
        String answerJson,
        String itemJson,
        Long templateVersionId,
        String schemaJson,
        AiReviewSummary aiReviewResult,
        AgentRunSummary agentRunSummary,
        List<ReviewRecordItem> reviewRecords,
        List<VersionHistoryItem> versionHistory
) {
    public record AiReviewSummary(
            Long aiReviewResultId,
            Long agentRunId,
            AiReviewStatus status,
            String decision,
            String averageScore,
            String riskFlags,
            String suggestion,
            String errorCode
    ) {}

    public record AgentRunSummary(
            Long agentRunId,
            String agentType,
            String modelName,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {}

    public record ReviewRecordItem(
            Long reviewRecordId,
            Long reviewerId,
            String action,
            Integer reviewLevel,
            String reason,
            String reviewComment,
            LocalDateTime createdAt
    ) {}

    public record VersionHistoryItem(
            Long submissionId,
            Integer versionNo,
            SubmissionStatus status,
            Boolean isGolden,
            LocalDateTime createdAt
    ) {}
}
