package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量软删除题目请求。
 */
@Schema(description = "批量软删除题目请求")
public record BatchDeleteItemsRequest(
        @NotEmpty @Schema(description = "待删除的题目ID列表") List<@NotNull Long> itemIds) {
}
