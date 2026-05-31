package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量软删除题目请求。
 */
public record BatchDeleteItemsRequest(@NotEmpty List<@NotNull Long> itemIds) {
}
