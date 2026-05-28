package com.labelhub.modules.assignment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MarketTaskResponse(Long taskId,
                                 String title,
                                 List<String> tags,
                                 LocalDateTime deadlineAt,
                                 Integer availableCount,
                                 Integer currentUserClaimedCount,
                                 RewardSummaryResponse rewardSummary) {
}
