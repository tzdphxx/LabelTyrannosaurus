package com.labelhub.modules.reward.dto;

import java.time.LocalDateTime;

/**
 * BE-A 审核通过事件。BE-B 只消费快照字段，不回写 submission 或 assignment 状态。
 */
public record SubmissionApprovedEvent(String eventId,
                                      Long taskId,
                                      Long datasetItemId,
                                      Long assignmentId,
                                      Long submissionId,
                                      Long labelerId,
                                      Long reviewerId,
                                      LocalDateTime approvedAt,
                                      String traceId) {
}
