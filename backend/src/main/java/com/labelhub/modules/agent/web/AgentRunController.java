package com.labelhub.modules.agent.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-runs")
public class AgentRunController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final AgentRunMapper agentRunMapper;
    private final ObjectMapper objectMapper;

    public AgentRunController(AgentRunMapper agentRunMapper, ObjectMapper objectMapper) {
        this.agentRunMapper = agentRunMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{agentRunId}")
    public ApiResponse<AgentRunDetailResponse> getDetail(@PathVariable Long agentRunId) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER));
        AgentRun run = agentRunMapper.selectById(agentRunId);
        if (run == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(toResponse(run));
    }

    private AgentRunDetailResponse toResponse(AgentRun run) {
        return new AgentRunDetailResponse(
                run.getId(), run.getAgentType(), run.getSubmissionId(),
                run.getProviderId(), run.getModelName(), run.getPromptVersion(),
                run.getStatus(),
                parseJson(run.getInputSnapshot()),
                parseJson(run.getOutputSnapshot()),
                run.getErrorMessage(),
                run.getStartedAt(), run.getFinishedAt(), run.getCreatedAt());
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, MAP_TYPE); }
        catch (Exception e) { return Map.of("raw", json); }
    }
}
