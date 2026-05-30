package com.labelhub.modules.ai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.tool.ReviewTool;
import com.labelhub.modules.ai.tool.ToolContext;
import com.labelhub.modules.ai.tool.ToolResult;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryTaskGuidelinesTool implements ReviewTool {

    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public QueryTaskGuidelinesTool(TaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "query_task_guidelines";
    }

    @Override
    public String description() {
        return "Get the task's labeling guidelines and instructions.";
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
            Task task = taskMapper.selectById(context.taskId());
            if (task == null) {
                return ToolResult.error("Task not found");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskTitle", task.getTitle());
            result.put("guidelines", task.getDescription());
            result.put("instructions", task.getInstructionRichText());
            return ToolResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return ToolResult.error("Failed to query guidelines: " + e.getMessage());
        }
    }
}
