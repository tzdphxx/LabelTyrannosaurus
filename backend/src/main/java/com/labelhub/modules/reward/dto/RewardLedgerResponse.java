package com.labelhub.modules.reward.dto;

import com.labelhub.modules.reward.domain.RewardDirection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardLedgerResponse(Long ledgerId,
                                   Long taskId,
                                   Long submissionId,
                                   Long assignmentId,
                                   BigDecimal amount,
                                   RewardDirection direction,
                                   String reason,
                                   String sourceEventId,
                                   String rewardType,
                                   LocalDateTime createdAt) {
}
