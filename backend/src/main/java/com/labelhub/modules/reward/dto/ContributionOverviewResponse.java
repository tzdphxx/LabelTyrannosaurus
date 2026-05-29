package com.labelhub.modules.reward.dto;

import java.math.BigDecimal;

public record ContributionOverviewResponse(Long labelerId,
                                           Integer claimedCount,
                                           Integer submittedCount,
                                           Integer pendingReviewCount,
                                           Integer approvedCount,
                                           Integer rejectedCount,
                                           BigDecimal totalReward,
                                           BigDecimal approvalRate) {
}
