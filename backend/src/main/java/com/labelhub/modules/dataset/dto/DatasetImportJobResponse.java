package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 数据集导入任务响应。
 *
 * <p>创建导入和查询导入共用该结构；错误报告字段仅在存在行级失败时返回。</p>
 */
@Schema(description = "数据集导入任务响应")
public record DatasetImportJobResponse(
        @Schema(description = "任务ID") Long jobId,
        @Schema(description = "关联的任务ID") Long taskId,
        @Schema(description = "任务状态") String status,
        @Schema(description = "导入模式") String importMode,
        @Schema(description = "总记录数") Integer totalCount,
        @Schema(description = "成功数量") Integer successCount,
        @Schema(description = "失败数量") Integer failedCount,
        @Schema(description = "错误报告文件ID") Long errorReportFileId,
        @Schema(description = "错误报告下载地址") String errorReportUrl,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "完成时间") LocalDateTime finishedAt,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
