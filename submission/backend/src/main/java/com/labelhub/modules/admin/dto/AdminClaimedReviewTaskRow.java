package com.labelhub.modules.admin.dto;

import java.time.LocalDateTime;

public record AdminClaimedReviewTaskRow(
        Long reviewerId,
        Long taskId,
        String title,
        Integer reviewLevel,
        Long pendingCount,
        LocalDateTime claimedAt
) {
    public ClaimedReviewTaskResponse toResponse() {
        return new ClaimedReviewTaskResponse(taskId, title, reviewLevel, pendingCount, claimedAt);
    }
}
