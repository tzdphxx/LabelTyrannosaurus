package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiDecision;
import com.labelhub.modules.submission.domain.SubmissionStatus;

public record SubmissionReviewItem(
        Long submissionId,
        Long taskId,
        Long datasetItemId,
        Long labelerId,
        SubmissionStatus submissionStatus,
        AiDecision aiDecision,
        String conflictStatus,
        int reviewLevel
) {
}
