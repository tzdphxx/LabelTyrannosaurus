package com.labelhub.modules.ai.tool;

public record ToolResult(boolean success, String output, String errorMessage) {

    public static ToolResult ok(String output) {
        return new ToolResult(true, output, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }
}
