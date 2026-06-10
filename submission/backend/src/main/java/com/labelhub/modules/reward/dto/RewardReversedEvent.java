package com.labelhub.modules.reward.dto;

import java.time.LocalDateTime;

/**
 * 奖励冲正事件。冲正只追加负向流水，不删除原正向流水。
 */
public record RewardReversedEvent(String eventId,
                                  Long taskId,
                                  Long submissionId,
                                  Long labelerId,
                                  String reason,
                                  Long operatorId,
                                  LocalDateTime createdAt,
                                  String traceId) {
}
