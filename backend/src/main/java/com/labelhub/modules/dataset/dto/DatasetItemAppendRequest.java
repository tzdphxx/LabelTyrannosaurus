package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 追加单条题目的请求。
 */
@Schema(description = "追加单条题目的请求")
public record DatasetItemAppendRequest(
        @NotBlank @Schema(description = "外部ID") String externalId,
        @NotNull @Schema(description = "题目JSON数据") Map<String, Object> itemJson,
        @Schema(description = "元数据JSON") Map<String, Object> metadataJson) {
}
