package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "导出分页请求")
public record ExportPageRequest(
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "上一页最后一条ID，用于游标分页") Long lastId,
        @Schema(description = "每页条数") int limit
) {
    public ExportPageRequest {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        if (lastId == null) {
            lastId = 0L;
        }
    }
}
