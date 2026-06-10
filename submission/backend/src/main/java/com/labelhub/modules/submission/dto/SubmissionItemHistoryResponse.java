package com.labelhub.modules.submission.dto;

import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SubmissionItemHistoryResponse(
        Long taskId,
        Long datasetItemId,
        List<HistoryItem> histories
) {
    public record HistoryItem(
            Long submissionId,
            Long assignmentId,
            Integer versionNo,
            SubmissionStatus status,
            Long submittedBy,
            String submittedByName,
            LocalDateTime submittedAt,
            AiReviewHistory aiReview,
            List<ReviewRoundHistory> reviewRounds
    ) {}

    public record AiReviewHistory(
            Long aiReviewResultId,
            Long agentRunId,
            AiReviewStatus status,
            String decision,
            LocalDateTime reviewedAt
    ) {}

    public record ReviewRoundHistory(
            Long reviewRecordId,
            Integer reviewLevel,
            Long reviewerId,
            String reviewerName,
            String action,
            String reason,
            String reviewComment,
            LocalDateTime reviewedAt
    ) {}
}
