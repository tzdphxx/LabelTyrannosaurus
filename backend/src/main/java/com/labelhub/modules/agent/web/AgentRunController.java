package com.labelhub.modules.agent.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.service.AgentRunQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-runs")
public class AgentRunController {

    private final AgentRunQueryService agentRunQueryService;

    public AgentRunController(AgentRunQueryService agentRunQueryService) {
        this.agentRunQueryService = agentRunQueryService;
    }

    @GetMapping("/{agentRunId}")
    public ApiResponse<AgentRunDetailResponse> getDetail(@PathVariable Long agentRunId) {
        return ApiResponse.ok(agentRunQueryService.getDetail(
                CurrentUserContext.requireCurrentUser(), agentRunId));
    }
}
