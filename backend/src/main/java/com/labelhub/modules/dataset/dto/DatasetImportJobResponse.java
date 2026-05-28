package com.labelhub.modules.dataset.dto;

import java.time.LocalDateTime;

public record DatasetImportJobResponse(Long jobId,
                                       Long taskId,
                                       String status,
                                       String importMode,
                                       Integer totalCount,
                                       Integer successCount,
                                       Integer failedCount,
                                       Long errorReportFileId,
                                       String errorReportUrl,
                                       String errorMessage,
                                       LocalDateTime startedAt,
                                       LocalDateTime finishedAt,
                                       LocalDateTime createdAt) {
}
