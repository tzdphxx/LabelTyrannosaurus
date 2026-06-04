package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 追加单条题目的请求。
 */
public record DatasetItemAppendRequest(
        @NotBlank String externalId,
        @NotNull Map<String, Object> itemJson,
        Map<String, Object> metadataJson
) {
}
