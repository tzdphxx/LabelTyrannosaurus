package com.labelhub.infrastructure.llm;

import java.util.List;

public record LlmMessage(
        String role,
        String content,
        List<ContentPart> contentParts,
        List<ToolCall> toolCalls,
        String toolCallId,
        String name
) {

    public LlmMessage(String role, String content) {
        this(role, content, null, null, null, null);
    }

    public static LlmMessage tool(String toolCallId, String name, String content) {
        return new LlmMessage("tool", content, null, null, toolCallId, name);
    }

    public static LlmMessage assistant(List<ToolCall> toolCalls) {
        return new LlmMessage("assistant", null, null, toolCalls, null, null);
    }

    public static LlmMessage userParts(List<ContentPart> contentParts) {
        return new LlmMessage("user", null, contentParts, null, null, null);
    }

    public sealed interface ContentPart permits TextPart, ImageUrlPart {
    }

    public record TextPart(String text) implements ContentPart {
    }

    public record ImageUrlPart(String url, String detail) implements ContentPart {
    }
}
