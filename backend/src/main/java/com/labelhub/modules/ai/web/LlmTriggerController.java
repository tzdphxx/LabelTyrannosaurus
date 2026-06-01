package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.service.LlmTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm/triggers")
@Tag(name = "大模型配置", description = "模板内大模型触发器运行")
public class LlmTriggerController {

    private final LlmTriggerService llmTriggerService;

    public LlmTriggerController(LlmTriggerService llmTriggerService) {
        this.llmTriggerService = llmTriggerService;
    }

    @PostMapping("/run")
    @Operation(summary = "运行 LLM 触发器", description = "基于当前用户上下文运行模板中的 LLM 触发器。")
    public ApiResponse<LlmTriggerRunResponse> run(@Valid @RequestBody LlmTriggerRunRequest request) {
        return ApiResponse.ok(llmTriggerService.run(CurrentUserContext.requireCurrentUser(), request));
    }

    @GetMapping("/runs/{triggerRunId}")
    @Operation(summary = "查询 LLM 触发器运行结果", description = "查询异步 LLM 触发器的运行状态、字段建议和错误信息。")
    public ApiResponse<LlmTriggerRunResponse> getRun(@PathVariable Long triggerRunId) {
        return ApiResponse.ok(llmTriggerService.getRun(CurrentUserContext.requireCurrentUser(), triggerRunId));
    }
}
