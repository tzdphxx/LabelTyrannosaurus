package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "LLM 对话消息")
public record LlmMessage(
        @Schema(description = "消息角色：system / user / assistant / tool", example = "user")
        String role,
        @Schema(description = "文本内容")
        String content,
        @Schema(description = "多模态内容部件列表")
        List<ContentPart> contentParts,
        @Schema(description = "工具调用列表")
        List<ToolCall> toolCalls,
        @Schema(description = "工具调用 ID")
        String toolCallId,
        @Schema(description = "名称")
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

    @Schema(description = "文本内容部件")
    public record TextPart(
            @Schema(description = "文本内容") String text
    ) implements ContentPart {
    }

    @Schema(description = "图片 URL 内容部件")
    public record ImageUrlPart(
            @Schema(description = "图片 URL") String url,
            @Schema(description = "图片分辨率级别", example = "auto") String detail
    ) implements ContentPart {
    }
}
