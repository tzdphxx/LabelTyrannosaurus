package com.labelhub.modules.ai.tool;

import com.labelhub.infrastructure.llm.ToolDefinition;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, ReviewTool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<ReviewTool> reviewTools) {
        for (ReviewTool tool : reviewTools) {
            tools.put(tool.name(), tool);
        }
    }

    public ReviewTool getTool(String name) {
        return tools.get(name);
    }

    public List<ToolDefinition> getToolDefinitions(List<String> enabledTools) {
        return tools.values().stream()
                .filter(t -> enabledTools == null || enabledTools.contains(t.name()))
                .map(t -> ToolDefinition.of(t.name(), t.description(), t.parametersSchema()))
                .toList();
    }

    public List<String> allToolNames() {
        return List.copyOf(tools.keySet());
    }

    public Map<String, ReviewTool> allTools() {
        return Collections.unmodifiableMap(tools);
    }
}
