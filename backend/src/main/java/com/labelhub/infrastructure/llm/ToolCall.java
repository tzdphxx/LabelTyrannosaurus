package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工具调用")
public record ToolCall(
        @Schema(description = "调用 ID")
        String id,
        @Schema(description = "调用类型", example = "function")
        String type,
        @Schema(description = "函数调用详情")
        FunctionCall function
) {
}
