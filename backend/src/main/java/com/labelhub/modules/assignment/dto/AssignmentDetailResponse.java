package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;

public record AssignmentDetailResponse(
        Long assignmentId,
        Long taskId,
        Long datasetItemId,
        Long templateVersionId,
        AssignmentStatus assignmentStatus,
        String schemaJson,
        String itemJson,
        String draftAnswerJson,
        Integer draftVersion,
        Long latestSubmissionId,
        SubmissionStatus latestSubmissionStatus,
        String returnedReason,
        LocalDateTime returnedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
