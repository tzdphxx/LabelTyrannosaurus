package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "工具定义")
public record ToolDefinition(
        @Schema(description = "工具类型", example = "function")
        String type,
        @Schema(description = "函数定义")
        FunctionDef function
) {

    public static ToolDefinition of(String name, String description, Map<String, Object> parameters) {
        return new ToolDefinition("function", new FunctionDef(name, description, parameters));
    }
}
