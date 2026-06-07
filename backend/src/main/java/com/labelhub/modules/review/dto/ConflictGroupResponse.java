package com.labelhub.modules.review.dto;

import com.labelhub.modules.review.domain.ConflictStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ConflictGroupResponse(
        Long groupId,
        Long taskId,
        Long datasetItemId,
        ConflictStatus status,
        BigDecimal consensusScore,
        Long goldenSubmissionId,
        List<CandidateSubmissionItem> candidateSubmissions,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public record CandidateSubmissionItem(
            Long submissionId,
            Long labelerId,
            String answerJson,
            AiReviewSummary aiReviewSummary,
            List<ReviewRecordItem> reviewRecords,
            Integer versionNo
    ) {}

    public record AiReviewSummary(
            Long aiReviewResultId,
            Long agentRunId,
            String status,
            String decision,
            String averageScore,
            String riskFlags,
            String suggestion
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
}
