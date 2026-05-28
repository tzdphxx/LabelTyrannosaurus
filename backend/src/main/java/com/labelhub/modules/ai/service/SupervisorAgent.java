package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.infrastructure.llm.FunctionCall;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.infrastructure.llm.OpenAiCompatibleAdapter;
import com.labelhub.infrastructure.llm.OpenAiCompatibleResponse;
import com.labelhub.infrastructure.llm.ToolCall;
import com.labelhub.infrastructure.llm.ToolDefinition;
import com.labelhub.modules.ai.tool.ReviewTool;
import com.labelhub.modules.ai.tool.ToolRegistry;
import com.labelhub.modules.ai.tool.ToolResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SupervisorAgent {

    private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);
    static final int MAX_TOOL_CALLS_PER_TURN = 5;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final OpenAiCompatibleAdapter adapter;
    private final LlmProviderService llmProviderService;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public SupervisorAgent(OpenAiCompatibleAdapter adapter,
                           LlmProviderService llmProviderService,
                           ToolRegistry toolRegistry,
                           ObjectMapper objectMapper) {
        this.adapter = adapter;
        this.llmProviderService = llmProviderService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public SupervisorResult execute(SupervisorRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", request.systemPrompt()));
        messages.add(new LlmMessage("user", request.userPrompt()));

        var runtimeConfig = llmProviderService
                .findEnabledRuntimeConfig(request.providerId(), request.modelName());
        if (runtimeConfig.isEmpty()) {
            return SupervisorResult.failure("PROVIDER_UNAVAILABLE", "LLM provider is unavailable", null);
        }

        for (int i = 0; i < request.maxIterations(); i++) {
            OpenAiCompatibleResponse response = adapter.chat(runtimeConfig.get(), messages, null, request.tools());
            if (response.timedOut()) {
                return SupervisorResult.failure("TIMEOUT", "Provider timed out", serializeMessages(messages));
            }
            if (!response.success()) {
                return SupervisorResult.failure("PROVIDER_ERROR", response.errorMessage(), serializeMessages(messages));
            }

            ParsedResponse parsed = parseResponse(response.rawResponse());
            if (parsed == null) {
                return SupervisorResult.failure("INVALID_RESPONSE", "Cannot parse LLM response", serializeMessages(messages));
            }

            if (parsed.toolCalls != null && !parsed.toolCalls.isEmpty()) {
                messages.add(LlmMessage.assistant(parsed.toolCalls));
                List<ToolCall> calls = parsed.toolCalls.size() > MAX_TOOL_CALLS_PER_TURN
                        ? parsed.toolCalls.subList(0, MAX_TOOL_CALLS_PER_TURN) : parsed.toolCalls;
                for (ToolCall call : calls) {
                    ToolResult result = executeTool(call, request.toolContext());
                    String output = result.success() ? result.output() : "ERROR: " + result.errorMessage();
                    messages.add(LlmMessage.tool(call.id(), call.function().name(), output));
                }
            } else if (parsed.content != null) {
                return parseResult(parsed.content, serializeMessages(messages));
            } else {
                return SupervisorResult.failure("EMPTY_RESPONSE", "LLM returned empty response", serializeMessages(messages));
            }
        }
        return SupervisorResult.failure("MAX_ITERATIONS", "Exceeded max iterations", serializeMessages(messages));
    }

    private ToolResult executeTool(ToolCall call, com.labelhub.modules.ai.tool.ToolContext context) {
        ReviewTool tool = toolRegistry.getTool(call.function().name());
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + call.function().name());
        }
        try {
            Map<String, Object> args = objectMapper.readValue(call.function().arguments(), OBJECT_MAP);
            return tool.execute(args, context);
        } catch (JsonProcessingException e) {
            return ToolResult.error("Invalid arguments JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Tool {} execution failed", call.function().name(), e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    private ParsedResponse parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode message = root.path("choices").path(0).path("message");
            if (message.isMissingNode()) {
                return null;
            }
            String content = message.has("content") && !message.get("content").isNull()
                    ? message.get("content").asText() : null;
            List<ToolCall> toolCalls = null;
            if (message.has("tool_calls") && message.get("tool_calls").isArray()) {
                toolCalls = new ArrayList<>();
                for (JsonNode tc : message.get("tool_calls")) {
                    String id = tc.path("id").asText();
                    String type = tc.path("type").asText("function");
                    String name = tc.path("function").path("name").asText();
                    String arguments = tc.path("function").path("arguments").asText();
                    toolCalls.add(new ToolCall(id, type, new FunctionCall(name, arguments)));
                }
            }
            return new ParsedResponse(content, toolCalls);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private SupervisorResult parseResult(String content, String rawConversation) {
        try {
            Map<String, Object> json = objectMapper.readValue(stripJsonFence(content), OBJECT_MAP);
            if (!json.containsKey("decision")) {
                return SupervisorResult.failure("INVALID_AI_REVIEW_OUTPUT",
                        "AI review decision is required", rawConversation);
            }
            String decision = String.valueOf(json.get("decision"));
            BigDecimal averageScore = json.containsKey("averageScore")
                    ? new BigDecimal(String.valueOf(json.get("averageScore"))) : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> dimensionScores = json.containsKey("dimensionScores")
                    ? (Map<String, Object>) json.get("dimensionScores") : null;
            @SuppressWarnings("unchecked")
            List<String> riskFlags = json.containsKey("riskFlags")
                    ? ((List<?>) json.get("riskFlags")).stream().map(String::valueOf).toList() : null;
            String suggestion = json.containsKey("suggestion") ? String.valueOf(json.get("suggestion")) : null;
            return SupervisorResult.success(decision, averageScore, dimensionScores, riskFlags, suggestion, rawConversation);
        } catch (Exception e) {
            return SupervisorResult.failure("INVALID_AI_REVIEW_OUTPUT",
                    "Cannot parse final answer: " + e.getMessage(), rawConversation);
        }
    }

    private String stripJsonFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFenceStart = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFenceStart <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
    }

    private String serializeMessages(List<LlmMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private record ParsedResponse(String content, List<ToolCall> toolCalls) {}
}