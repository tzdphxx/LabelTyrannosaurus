package com.labelhub.modules.export.dto;

import java.time.LocalDateTime;

/**
 * 导出任务响应。
 */
public record ExportJobResponse(Long exportJobId,
                                Long taskId,
                                String exportFormat,
                                String status,
                                Boolean includeAiReview,
                                Boolean includeAuditTrail,
                                Boolean includeReviewComment,
                                Boolean includeLabelerInfo,
                                String fieldMappingJson,
                                Long resultFileId,
                                String downloadUrl,
                                String errorMessage,
                                String traceId,
                                LocalDateTime startedAt,
                                LocalDateTime finishedAt,
                                LocalDateTime createdAt) {
}
