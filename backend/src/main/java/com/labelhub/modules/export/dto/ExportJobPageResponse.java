package com.labelhub.modules.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 导出任务分页响应。
 */
@Schema(description = "导出任务分页响应")
public record ExportJobPageResponse(
        @Schema(description = "导出任务列表") List<ExportJobResponse> items,
        @Schema(description = "当前页码") int page,
        @Schema(description = "每页条数") int pageSize,
        @Schema(description = "总记录数") long total) {
}
