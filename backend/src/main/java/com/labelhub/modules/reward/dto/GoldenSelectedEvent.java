package com.labelhub.modules.reward.dto;

import java.time.LocalDateTime;

/**
 * BE-A 冲突仲裁金标事件。assignmentId 不在事件内，结算时需只读查询 submission 快照补齐。
 */
public record GoldenSelectedEvent(String eventId,
                                  Long goldenSubmissionId,
                                  String conflictGroupId,
                                  Long reviewerId,
                                  LocalDateTime resolvedAt,
                                  String traceId) {
}
