package com.labelhub.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "媒体处理任务响应")
public record MediaProcessingJobResponse(
        @Schema(description = "任务ID") Long jobId,
        @Schema(description = "数据集题目ID") Long datasetItemId,
        @Schema(description = "关联任务ID") Long taskId,
        @Schema(description = "处理状态") String status,
        @Schema(description = "总资源数") Integer totalAssets,
        @Schema(description = "已处理资源数") Integer processedAssets,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "完成时间") LocalDateTime finishedAt,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
