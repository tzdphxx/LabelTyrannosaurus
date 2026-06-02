package com.labelhub.modules.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 导出任务响应。
 */
@Schema(description = "导出任务响应")
public record ExportJobResponse(
        @Schema(description = "导出任务ID") Long exportJobId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "导出格式") String exportFormat,
        @Schema(description = "任务状态") String status,
        @Schema(description = "是否包含AI审核") Boolean includeAiReview,
        @Schema(description = "是否包含审计追踪") Boolean includeAuditTrail,
        @Schema(description = "是否包含审核评论") Boolean includeReviewComment,
        @Schema(description = "是否包含标注员信息") Boolean includeLabelerInfo,
        @Schema(description = "字段映射JSON") String fieldMappingJson,
        @Schema(description = "结果文件ID") Long resultFileId,
        @Schema(description = "下载地址") String downloadUrl,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "追踪ID") String traceId,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "完成时间") LocalDateTime finishedAt,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
