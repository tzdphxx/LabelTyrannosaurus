package com.labelhub.modules.agent.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.service.AgentRunQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-runs")
@Tag(name = "Agent 运行记录", description = "查询 AI Agent 的运行详情，包括输入快照、输出快照、状态和耗时")
public class AgentRunController {

    private final AgentRunQueryService agentRunQueryService;

    public AgentRunController(AgentRunQueryService agentRunQueryService) {
        this.agentRunQueryService = agentRunQueryService;
    }

    @GetMapping("/{agentRunId}")
    @Operation(summary = "Agent 运行详情", description = "根据 agentRunId 查询单次 Agent 运行的完整信息，包括输入 Prompt 快照、LLM 输出、状态、耗时等。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AgentRunDetailResponse> getDetail(
            @Parameter(description = "Agent 运行记录 ID") @PathVariable Long agentRunId) {
        return ApiResponse.ok(agentRunQueryService.getDetail(
                CurrentUserContext.requireCurrentUser(), agentRunId));
    }
}
