package com.labelhub.modules.export.dto;

import com.labelhub.modules.export.domain.ExportFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 创建导出任务请求。
 */
@Schema(description = "创建导出任务请求")
public record CreateExportRequest(
        @Schema(description = "导出格式") ExportFormat exportFormat,
        @Schema(description = "是否包含AI审核") Boolean includeAiReview,
        @Schema(description = "是否包含审计追踪") Boolean includeAuditTrail,
        @Schema(description = "是否包含审核评论") Boolean includeReviewComment,
        @Schema(description = "是否包含标注员信息") Boolean includeLabelerInfo,
        @Schema(description = "字段映射列表") List<ExportFieldMapping> fieldMappings) {
}
