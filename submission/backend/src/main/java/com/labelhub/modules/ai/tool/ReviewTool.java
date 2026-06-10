package com.labelhub.modules.ai.tool;

import java.util.Map;

public interface ReviewTool {

    String name();

    String description();

    Map<String, Object> parametersSchema();

    ToolResult execute(Map<String, Object> arguments, ToolContext context);
}
