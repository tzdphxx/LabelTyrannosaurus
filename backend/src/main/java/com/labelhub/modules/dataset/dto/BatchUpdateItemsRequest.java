package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量更新题目请求。
 */
@Schema(description = "批量更新题目请求")
public record BatchUpdateItemsRequest(
        @NotEmpty @Schema(description = "待更新的题目列表") List<@NotNull @Valid DatasetItemUpdateRequest> items) {
}
