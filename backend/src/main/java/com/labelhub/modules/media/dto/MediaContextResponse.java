package com.labelhub.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "媒体上下文响应")
public record MediaContextResponse(
        @Schema(description = "数据集题目ID") Long datasetItemId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "媒体类型") String mediaType,
        @Schema(description = "处理状态") String processingStatus,
        @Schema(description = "上下文数据") Map<String, Object> context,
        @Schema(description = "限制列表") List<String> limitations,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
