package com.labelhub.modules.submission.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;

public record LabelerSubmissionListItem(
        Long submissionId,
        Long assignmentId,
        Long taskId,
        Long datasetItemId,
        Integer versionNo,
        SubmissionStatus submissionStatus,
        AssignmentStatus assignmentStatus,
        AiReviewStatus aiReviewStatus,
        String aiDecision,
        String reviewSummary,
        String rejectReason,
        Boolean isGolden,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
