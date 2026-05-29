package com.labelhub.infrastructure.llm;

import java.util.Map;

public record ToolDefinition(String type, FunctionDef function) {

    public static ToolDefinition of(String name, String description, Map<String, Object> parameters) {
        return new ToolDefinition("function", new FunctionDef(name, description, parameters));
    }
}
