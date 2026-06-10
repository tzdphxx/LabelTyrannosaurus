package com.labelhub.modules.export.dto;

import com.labelhub.modules.export.domain.ExportFormat;

import java.util.List;

/**
 * 创建导出任务请求。
 */
public record CreateExportRequest(ExportFormat exportFormat,
                                  Boolean includeAiReview,
                                  Boolean includeAuditTrail,
                                  Boolean includeReviewComment,
                                  Boolean includeLabelerInfo,
                                  List<ExportFieldMapping> fieldMappings) {
}
