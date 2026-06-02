package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "函数调用详情")
public record FunctionCall(
        @Schema(description = "被调用函数名称")
        String name,
        @Schema(description = "函数参数 JSON 字符串")
        String arguments
) {
}
