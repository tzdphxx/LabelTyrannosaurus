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
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/llm-providers")
@Tag(name = "LLM 厂商管理", description = "管理员管理全局 LLM 厂商配置，包括创建、编辑、启用、停用和测试连接")
public class AdminLlmProviderController {

    private final LlmProviderService llmProviderService;

    public AdminLlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @GetMapping
    @Operation(summary = "Provider 管理列表",
            description = "ADMIN 查看全部 LLM Provider，包括已启用和已停用配置；响应不会返回 API Key 明文或密文。")
    public ApiResponse<List<LlmProviderResponse>> list() {
        CurrentUserContext.requireRole(RoleCode.ADMIN);
        return ApiResponse.ok(llmProviderService.listAllForAdmin());
    }

    @PostMapping
    @Operation(summary = "创建 Provider", description = "ADMIN 创建全局 LLM Provider 配置。")
    public ApiResponse<LlmProviderResponse> create(@Valid @RequestBody CreateLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.create(adminId, request));
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "更新 Provider", description = "ADMIN 更新全局 LLM Provider 配置。")
    public ApiResponse<LlmProviderResponse> update(@PathVariable Long providerId,
                                                   @Valid @RequestBody UpdateLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.update(adminId, providerId, request));
    }

    @PostMapping("/{providerId}/enable")
    @Operation(summary = "启用 Provider", description = "ADMIN 启用全局 LLM Provider。")
    public ApiResponse<LlmProviderResponse> enable(@PathVariable Long providerId) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.enable(adminId, providerId));
    }

    @PostMapping("/{providerId}/disable")
    @Operation(summary = "停用 Provider", description = "ADMIN 停用全局 LLM Provider。")
    public ApiResponse<LlmProviderResponse> disable(@PathVariable Long providerId) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.disable(adminId, providerId));
    }

    @PostMapping("/{providerId}/test")
    @Operation(summary = "测试 Provider", description = "ADMIN 测试全局 LLM Provider 连通性。")
    public ApiResponse<LlmProviderTestResponse> test(@PathVariable Long providerId,
                                                     @Valid @RequestBody TestLlmProviderRequest request) {
        Long adminId = CurrentUserContext.requireRole(RoleCode.ADMIN).userId();
        return ApiResponse.ok(llmProviderService.test(adminId, providerId, request));
    }
}
