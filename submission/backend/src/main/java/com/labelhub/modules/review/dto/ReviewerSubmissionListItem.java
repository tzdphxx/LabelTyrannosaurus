package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;

public record ReviewerSubmissionListItem(
        Long submissionId,
        Long taskId,
        Long datasetItemId,
        Long labelerId,
        SubmissionStatus submissionStatus,
        AiReviewStatus aiReviewStatus,
        String aiDecision,
        String conflictStatus,
        Integer reviewLevel,
        Long assignedReviewerId,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
) {
}
