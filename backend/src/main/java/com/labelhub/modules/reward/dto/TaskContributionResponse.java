package com.labelhub.modules.reward.dto;

import java.math.BigDecimal;

public record TaskContributionResponse(Long taskId,
                                       String taskTitle,
                                       Integer submittedCount,
                                       Integer approvedCount,
                                       Integer rejectedCount,
                                       BigDecimal totalReward) {
}
