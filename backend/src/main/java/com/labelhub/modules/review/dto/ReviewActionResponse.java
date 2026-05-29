package com.labelhub.modules.review.dto;

import com.labelhub.modules.submission.domain.SubmissionStatus;

public record ReviewActionResponse(
        Long submissionId,
        SubmissionStatus submissionStatus,
        Long reviewRecordId
) {
}
