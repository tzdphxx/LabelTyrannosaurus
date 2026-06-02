package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "发布者任务分页响应")
public record OwnerTaskPageResponse(
        @Schema(description = "任务摘要列表")
        List<OwnerTaskSummaryResponse> items,
        @Schema(description = "当前页码", example = "1")
        int page,
        @Schema(description = "每页条数", example = "20")
        int pageSize,
        @Schema(description = "总条数", example = "100")
        long total) {
}
