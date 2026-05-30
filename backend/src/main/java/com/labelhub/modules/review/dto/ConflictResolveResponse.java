package com.labelhub.modules.review.dto;

import com.labelhub.modules.review.domain.ConflictStatus;

public record ConflictResolveResponse(
        Long groupId,
        ConflictStatus status,
        Long goldenSubmissionId,
        Long reviewRecordId
) {
}
