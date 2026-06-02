package com.labelhub.modules.task.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskReviewerResponse(
        Long reviewerId,
        String username,
        String displayName,
        LocalDateTime assignedAt
) {
}
