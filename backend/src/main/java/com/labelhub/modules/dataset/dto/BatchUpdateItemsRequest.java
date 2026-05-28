package com.labelhub.modules.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量更新题目请求。
 */
public record BatchUpdateItemsRequest(@NotEmpty @Valid List<DatasetItemUpdateRequest> items) {
}
