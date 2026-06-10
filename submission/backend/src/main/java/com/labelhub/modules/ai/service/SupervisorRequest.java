package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llm.ToolDefinition;
import com.labelhub.modules.ai.tool.ToolContext;
import java.util.List;

public record SupervisorRequest(
        Long submissionId,
        Long taskId,
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools,
        ToolContext toolContext,
        int maxIterations,
        Long providerId,
        String modelName
) {
}
