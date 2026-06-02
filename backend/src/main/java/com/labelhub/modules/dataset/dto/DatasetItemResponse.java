package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 题目列表响应项。
 */
@Schema(description = "题目列表响应项")
public record DatasetItemResponse(
        @Schema(description = "题目ID") Long itemId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "外部ID") String externalId,
        @Schema(description = "题目JSON数据") JsonNode itemJson,
        @Schema(description = "元数据JSON") JsonNode metadataJson,
        @Schema(description = "已分配数量") Integer assignedCount,
        @Schema(description = "已提交数量") Integer submittedCount,
        @Schema(description = "已通过数量") Integer approvedCount,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt) {
}
