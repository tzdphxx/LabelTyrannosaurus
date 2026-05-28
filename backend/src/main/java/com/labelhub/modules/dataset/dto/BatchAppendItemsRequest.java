package com.labelhub.modules.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量追加题目请求。
 */
public record BatchAppendItemsRequest(@NotEmpty @Valid List<DatasetItemAppendRequest> items) {
}
