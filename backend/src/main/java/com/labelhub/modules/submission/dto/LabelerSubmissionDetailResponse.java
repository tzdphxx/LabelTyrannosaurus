package com.labelhub.modules.submission.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LabelerSubmissionDetailResponse(
        Long submissionId,
        Long assignmentId,
        Long taskId,
        Long datasetItemId,
        Long templateVersionId,
        Integer versionNo,
        SubmissionStatus submissionStatus,
        AssignmentStatus assignmentStatus,
        String itemJson,
        String schemaJson,
        String answerJson,
        AiReviewStatus aiReviewStatus,
        String aiDecision,
        String aiSuggestion,
        String rejectReason,
        List<ReviewRecordSummary> reviewRecords,
        List<VersionSummary> versionHistory,
        boolean canModify
) {
    public record ReviewRecordSummary(
            Long reviewRecordId,
            String action,
            String reason,
            LocalDateTime createdAt
    ) {}

    public record VersionSummary(
            Long submissionId,
            Integer versionNo,
            SubmissionStatus status,
            LocalDateTime createdAt
    ) {}
}
