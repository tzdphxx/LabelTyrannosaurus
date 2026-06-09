package com.labelhub.modules.task.dto;

import java.time.LocalDateTime;

public record TaskLabelerResponse(
        Long labelerId,
        String username,
        String displayName,
        int claimedCount,
        int submittedCount,
        int approvedCount,
        int rejectedCount,
        int cancelledCount,
        LocalDateTime firstClaimedAt,
        LocalDateTime lastActivityAt
) {
}
