package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "函数定义")
public record FunctionDef(
        @Schema(description = "函数名称", example = "classify_image")
        String name,
        @Schema(description = "函数描述")
        String description,
        @Schema(description = "参数 JSON Schema")
        Map<String, Object> parameters
) {
}
