package com.labelhub.modules.submission.dto;

import java.time.LocalDateTime;

/**
 * 导出快照中的审计引用。
 */
public record AuditRef(Long auditId,
                       Long submissionId,
                       String action,
                       String traceId,
                       LocalDateTime createdAt) {
}
