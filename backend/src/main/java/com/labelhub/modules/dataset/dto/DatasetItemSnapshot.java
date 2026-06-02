package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 给 BE-A 领取、提交和渲染链路使用的题目快照。
 *
 * <p>快照只表达数据集资产的稳定内容，不携带 assignment 或 submission 状态。</p>
 */
@Schema(description = "题目快照，用于领取、提交和渲染链路")
public record DatasetItemSnapshot(
        @Schema(description = "题目ID") Long itemId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "外部ID") String externalId,
        @Schema(description = "题目JSON数据") JsonNode itemJson,
        @Schema(description = "元数据JSON") JsonNode metadataJson) {
}
