package com.labelhub.modules.dataset.dto;

import com.labelhub.modules.dataset.domain.DatasetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 追加单条题目的请求。
 */
public record DatasetItemAppendRequest(@NotBlank String externalId,
                                       @NotNull DatasetType datasetType,
                                       @NotNull Map<String, Object> itemJson,
                                       Map<String, Object> metadataJson) {
}
