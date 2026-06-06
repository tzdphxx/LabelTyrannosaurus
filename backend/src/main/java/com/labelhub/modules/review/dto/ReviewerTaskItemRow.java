package com.labelhub.modules.review.dto;

import java.time.LocalDateTime;

public record ReviewerTaskItemRow(
        Long datasetItemId,
        String externalId,
        String itemJson,
        String metadataJson,
        String itemStatus,
        Long assignmentId,
        String assignmentStatus,
        Long labelerId,
        String labelerName,
        Long latestSubmissionId,
        Integer versionNo,
        String submissionStatus,
        LocalDateTime submittedAt,
        String aiReviewStatus,
        String aiDecision,
        String averageScore,
        String riskFlags,
        String suggestion,
        String reviewTaskStatus,
        Integer reviewLevel,
        String latestReviewAction,
        LocalDateTime latestReviewAt,
        boolean canOpenSubmissionDetail,
        boolean canReview
) {
}
