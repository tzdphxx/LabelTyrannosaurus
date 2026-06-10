package com.labelhub.modules.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量更新题目请求。
 */
public record BatchUpdateItemsRequest(@NotEmpty List<@NotNull @Valid DatasetItemUpdateRequest> items) {
}
