package com.labelhub.infrastructure.llm;

public record ToolCall(String id, String type, FunctionCall function) {
}
