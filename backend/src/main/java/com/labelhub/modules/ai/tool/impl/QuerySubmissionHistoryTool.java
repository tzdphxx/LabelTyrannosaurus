package com.labelhub.modules.ai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.tool.ReviewTool;
import com.labelhub.modules.ai.tool.ToolContext;
import com.labelhub.modules.ai.tool.ToolResult;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QuerySubmissionHistoryTool implements ReviewTool {

    private final SubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;

    public QuerySubmissionHistoryTool(SubmissionMapper submissionMapper, ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "query_submission_history";
    }

    @Override
    public String description() {
        return "Query the labeler's recent submission history and pass rate for this task.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of("type", "integer", "description", "Max results to return", "default", 10)
                ),
                "required", List.of()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        try {
            int limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 10;
            List<Submission> history = submissionMapper.selectRecentByLabeler(
                    context.labelerId(), context.taskId(), limit);
            long total = history.size();
            long passed = history.stream()
                    .filter(s -> "APPROVED".equals(s.getStatus().name()))
                    .count();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalSubmissions", total);
            result.put("passedCount", passed);
            result.put("passRate", total > 0 ? String.format("%.1f%%", passed * 100.0 / total) : "N/A");
            return ToolResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return ToolResult.error("Failed to query history: " + e.getMessage());
        }
    }
}
