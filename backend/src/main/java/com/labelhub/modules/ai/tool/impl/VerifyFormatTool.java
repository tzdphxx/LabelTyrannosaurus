package com.labelhub.modules.ai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.tool.ReviewTool;
import com.labelhub.modules.ai.tool.ToolContext;
import com.labelhub.modules.ai.tool.ToolResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class VerifyFormatTool implements ReviewTool {

    private final ObjectMapper objectMapper;

    public VerifyFormatTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "verify_format";
    }

    @Override
    public String description() {
        return "Verify that the submission answer JSON conforms to the expected output schema structure.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        try {
            if (context.answerJson() == null || context.answerJson().isBlank()) {
                return ToolResult.ok("{\"valid\":false,\"reason\":\"Answer is empty\"}");
            }
            objectMapper.readTree(context.answerJson());
            return ToolResult.ok("{\"valid\":true,\"reason\":\"Answer is valid JSON\"}");
        } catch (Exception e) {
            return ToolResult.ok("{\"valid\":false,\"reason\":\"Answer is not valid JSON: " + e.getMessage() + "\"}");
        }
    }
}
