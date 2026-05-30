package com.labelhub.modules.dataset.dto;

import java.time.LocalDateTime;

/**
 * 数据集导入任务响应。
 *
 * <p>创建导入和查询导入共用该结构；错误报告字段仅在存在行级失败时返回。</p>
 */
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
