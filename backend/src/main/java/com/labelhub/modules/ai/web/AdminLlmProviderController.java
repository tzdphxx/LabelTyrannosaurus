package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.CreateLlmProviderRequest;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.dto.TestLlmProviderRequest;
import com.labelhub.modules.ai.dto.UpdateLlmProviderRequest;
import com.labelhub.modules.ai.service.LlmProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/llm-providers")
@Tag(name = "LLM 模型供应商管理", description = "管理员创建、更新、启停和测试全局 LLM Provider")
public class AdminLlmProviderController {

    private final LlmProviderService llmProviderService;

    public AdminLlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @PostMapping
    @Operation(summary = "创建模型供应商", description = "管理员创建全局 LLM Provider 配置，API Key 会加密保存。")
    public ApiResponse<LlmProviderResponse> create(@Valid @RequestBody CreateLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.create(adminId, request));
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "更新模型供应商", description = "管理员更新指定 LLM Provider 配置；apiKey 为空时保留原密钥。")
    public ApiResponse<LlmProviderResponse> update(@PathVariable Long providerId,
                                                   @Valid @RequestBody UpdateLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.update(adminId, providerId, request));
    }

    @PostMapping("/{providerId}/enable")
    @Operation(summary = "启用模型供应商", description = "管理员启用全局 LLM Provider，启用后 Owner 可在任务配置中选择。")
    public ApiResponse<LlmProviderResponse> enable(@PathVariable Long providerId) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.enable(adminId, providerId));
    }

    @PostMapping("/{providerId}/disable")
    @Operation(summary = "停用模型供应商", description = "管理员停用全局 LLM Provider，停用后不再允许新任务使用。")
    public ApiResponse<LlmProviderResponse> disable(@PathVariable Long providerId) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.disable(adminId, providerId));
    }

    @PostMapping("/{providerId}/test")
    @Operation(summary = "测试模型供应商", description = "管理员使用指定模型和密钥测试 LLM Provider 连通性，返回测试是否成功和耗时。")
    public ApiResponse<LlmProviderTestResponse> test(@PathVariable Long providerId,
                                                     @Valid @RequestBody TestLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.test(adminId, providerId, request));
    }
}
