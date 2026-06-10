package com.labelhub.modules.submission.repository;

import java.time.LocalDateTime;

/**
 * 审计引用查询记录。
 */
public record AuditRefRecord(Long auditId,
                             Long submissionId,
                             String action,
                             String traceId,
                             LocalDateTime createdAt) {
}
