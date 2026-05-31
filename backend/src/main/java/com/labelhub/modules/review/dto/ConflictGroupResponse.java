package com.labelhub.modules.review.dto;

import com.labelhub.modules.review.domain.ConflictStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConflictGroupResponse(
        Long groupId,
        Long taskId,
        Long datasetItemId,
        ConflictStatus status,
        BigDecimal consensusScore,
        Long goldenSubmissionId,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
