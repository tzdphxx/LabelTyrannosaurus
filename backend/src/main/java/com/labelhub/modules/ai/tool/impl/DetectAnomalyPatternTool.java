package com.labelhub.modules.ai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.tool.ReviewTool;
import com.labelhub.modules.ai.tool.ToolContext;
import com.labelhub.modules.ai.tool.ToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DetectAnomalyPatternTool implements ReviewTool {

    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.{3,})\\1{3,}");

    private final ObjectMapper objectMapper;

    public DetectAnomalyPatternTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "detect_anomaly_pattern";
    }

    @Override
    public String description() {
        return "Detect anomaly patterns in the answer such as repetition, all-same selections, or suspiciously short answers.";
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
            String answer = context.answerJson();
            Map<String, Object> result = new LinkedHashMap<>();
            List<String> anomalies = new java.util.ArrayList<>();

            if (answer == null || answer.length() < 5) {
                anomalies.add("SUSPICIOUSLY_SHORT");
            }
            if (answer != null && REPEATED_PATTERN.matcher(answer).find()) {
                anomalies.add("REPEATED_CONTENT");
            }
            result.put("anomaliesDetected", !anomalies.isEmpty());
            result.put("anomalies", anomalies);
            return ToolResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return ToolResult.error("Anomaly detection failed: " + e.getMessage());
        }
    }
}
