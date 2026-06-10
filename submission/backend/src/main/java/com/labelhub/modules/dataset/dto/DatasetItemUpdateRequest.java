package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 更新单条题目的请求。
 */
public record DatasetItemUpdateRequest(@NotNull Long itemId,
                                       @NotNull Map<String, Object> itemJson,
                                       Map<String, Object> metadataJson) {
}
