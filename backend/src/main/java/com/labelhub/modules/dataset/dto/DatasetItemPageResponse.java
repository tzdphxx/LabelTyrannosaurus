package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 题目分页响应。
 */
@Schema(description = "题目分页响应")
public record DatasetItemPageResponse(
        @Schema(description = "题目列表") List<DatasetItemResponse> items,
        @Schema(description = "当前页码") int page,
        @Schema(description = "每页条数") int pageSize,
        @Schema(description = "总记录数") long total) {
}
