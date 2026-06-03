package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.service.LlmTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "LLM 触发器", description = "字段级 AI 辅助：标注作答时触发，或 Owner 搭建模板时预览测试")
public class LlmTriggerController {

    private final LlmTriggerService llmTriggerService;

    public LlmTriggerController(LlmTriggerService llmTriggerService) {
        this.llmTriggerService = llmTriggerService;
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/llm-triggers")
    @Operation(summary = "标注时触发 LLM 辅助", description = "标注员在作答过程中点击按钮，前端全量传入模型和 Prompt 参数。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmTriggerRunResponse> runForAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody LlmTriggerRunRequest request) {
        return ApiResponse.ok(llmTriggerService.runForAssignment(
                CurrentUserContext.requireCurrentUser(), assignmentId, request));
    }

    @PostMapping("/api/v1/tasks/{taskId}/llm-triggers/test")
    @Operation(summary = "Owner 预览测试 LLM 辅助", description = "Owner 搭模板时使用指定题目测试 LLM 触发器的 Prompt 效果。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmTriggerRunResponse> testFromTask(
            @PathVariable Long taskId,
            @Valid @RequestBody LlmTriggerRunRequest request) {
        return ApiResponse.ok(llmTriggerService.testFromTask(
                CurrentUserContext.requireCurrentUser(), taskId, request));
    }

    @GetMapping("/api/v1/llm/triggers/runs/{triggerRunId}")
    @Operation(summary = "查询 LLM 辅助运行结果", description = "轮询异步 LLM 触发器的运行状态、建议内容和失败原因。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmTriggerRunResponse> getRun(@PathVariable Long triggerRunId) {
        return ApiResponse.ok(llmTriggerService.getRun(
                CurrentUserContext.requireCurrentUser(), triggerRunId));
    }
}
