package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.AiReviewPromptTestRequest;
import com.labelhub.modules.ai.dto.AiReviewPromptTestResponse;
import com.labelhub.modules.ai.service.AiReviewConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/ai-review-configs")
@Tag(name = "AI 审核", description = "任务 AI 审核配置和提示词测试")
public class AiReviewConfigController {

    private final AiReviewConfigService aiReviewConfigService;

    public AiReviewConfigController(AiReviewConfigService aiReviewConfigService) {
        this.aiReviewConfigService = aiReviewConfigService;
    }

    @PostMapping
    @Operation(summary = "保存 AI 审核配置", description = """
            创建或保存任务 AI 审核配置。
            providerId、promptTemplate、scoringDimensions、passThreshold、manualReviewThreshold 必填；
            modelName 可选，缺省或空白时使用所选 Provider 的 defaultModel。
            aiFlowPolicy 用于描述 AI 结果是否可直接过审/打回，缺省为 MANUAL_FIRST。""")
    public ApiResponse<AiReviewConfigResponse> save(@PathVariable Long taskId,
                                                    @Valid @RequestBody AiReviewConfigRequest request) {
        return ApiResponse.ok(aiReviewConfigService.save(CurrentUserContext.getUserId(), taskId, request));
    }

    @PutMapping("/{configId}")
    @Operation(summary = "更新 AI 审核配置", description = """
            更新指定 AI 审核配置。
            modelName 可选，缺省或空白时使用所选 Provider 的 defaultModel；
            修改后 promptVersion 会递增，任务仍需处于 DRAFT 状态。""")
    public ApiResponse<AiReviewConfigResponse> update(@PathVariable Long taskId,
                                                      @PathVariable Long configId,
                                                      @Valid @RequestBody AiReviewConfigRequest request) {
        return ApiResponse.ok(aiReviewConfigService.update(CurrentUserContext.getUserId(), taskId, configId,
                request));
    }

    @GetMapping
    @Operation(summary = "获取 AI 审核配置", description = "查询任务当前 AI 审核配置。")
    public ApiResponse<AiReviewConfigResponse> get(@PathVariable Long taskId) {
        return ApiResponse.ok(aiReviewConfigService.get(CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{configId}/test")
    @Operation(summary = "测试 AI 审核提示词", description = "用样例输入测试 AI 审核提示词和输出结构。")
    public ApiResponse<AiReviewPromptTestResponse> test(@PathVariable Long taskId,
                                                        @PathVariable Long configId,
                                                        @Valid @RequestBody AiReviewPromptTestRequest request) {
        return ApiResponse.ok(aiReviewConfigService.testPrompt(CurrentUserContext.getUserId(), taskId, configId,
                request));
    }
}
