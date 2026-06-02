package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "大模型触发运行分页响应")
public record LlmTriggerRunPageResponse(
        @Schema(description = "触发运行列表")
        List<LlmTriggerRunResponse> items,
        @Schema(description = "当前页码")
        int page,
        @Schema(description = "每页大小")
        int pageSize,
        @Schema(description = "总记录数")
        long total
) {
}
