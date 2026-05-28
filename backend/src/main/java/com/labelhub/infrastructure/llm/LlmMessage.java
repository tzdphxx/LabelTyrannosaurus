package com.labelhub.infrastructure.llm;

import java.util.List;

public record LlmMessage(
        String role,
        String content,
        List<ToolCall> toolCalls,
        String toolCallId,
        String name
) {

    public LlmMessage(String role, String content) {
        this(role, content, null, null, null);
    }

    public static LlmMessage tool(String toolCallId, String name, String content) {
        return new LlmMessage("tool", content, null, toolCallId, name);
    }

    public static LlmMessage assistant(List<ToolCall> toolCalls) {
        return new LlmMessage("assistant", null, toolCalls, null, null);
    }
}
