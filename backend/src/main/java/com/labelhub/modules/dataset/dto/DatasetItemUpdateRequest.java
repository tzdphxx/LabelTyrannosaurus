package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 更新单条题目的请求。
 */
@Schema(description = "更新单条题目的请求")
public record DatasetItemUpdateRequest(
        @NotNull @Schema(description = "题目ID") Long itemId,
        @NotNull @Schema(description = "题目JSON数据") Map<String, Object> itemJson,
        @Schema(description = "元数据JSON") Map<String, Object> metadataJson) {
}
