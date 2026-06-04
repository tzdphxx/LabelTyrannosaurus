package com.labelhub.modules.admin.dto;

import java.time.LocalDateTime;

public record ClaimedReviewTaskResponse(
        Long taskId,
        String title,
        Integer reviewLevel,
        Long pendingCount,
        LocalDateTime claimedAt
) {
}
