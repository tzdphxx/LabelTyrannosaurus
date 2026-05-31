package com.labelhub.modules.reward.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyContributionPoint(LocalDate statDate,
                                     Integer submittedCount,
                                     Integer approvedCount,
                                     Integer rejectedCount,
                                     BigDecimal rewardAmount) {
}
