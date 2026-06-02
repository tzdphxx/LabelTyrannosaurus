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

import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/llm-providers")
@Tag(name = "大模型配置", description = "OWNER 维护大模型供应商")
public class LlmProviderController {

    private final LlmProviderService llmProviderService;

    public LlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @GetMapping
    @Operation(summary = "供应商列表", description = "查询大模型供应商配置列表。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<LlmProviderResponse>> list() {
        Long ownerId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.list(ownerId));
    }

    @PostMapping
    @Operation(summary = "创建供应商", description = "创建大模型供应商配置。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmProviderResponse> create(@Valid @RequestBody CreateLlmProviderRequest request) {
        Long actorId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.create(actorId, request));
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "更新供应商", description = "更新大模型供应商配置。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmProviderResponse> update(@PathVariable Long providerId,
                                                   @Valid @RequestBody UpdateLlmProviderRequest request) {
        Long actorId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.update(actorId, providerId, request));
    }

    @PostMapping("/{providerId}/enable")
    @Operation(summary = "启用供应商", description = "启用大模型供应商。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmProviderResponse> enable(@PathVariable Long providerId) {
        Long actorId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.enable(actorId, providerId));
    }

    @PostMapping("/{providerId}/disable")
    @Operation(summary = "禁用供应商", description = "禁用大模型供应商。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmProviderResponse> disable(@PathVariable Long providerId) {
        Long actorId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.disable(actorId, providerId));
    }

    @PostMapping("/{providerId}/test")
    @Operation(summary = "测试供应商", description = "测试大模型供应商连通性。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LlmProviderTestResponse> test(@PathVariable Long providerId,
                                                     @Valid @RequestBody TestLlmProviderRequest request) {
        Long ownerId = CurrentUserContext.requireRole(RoleCode.OWNER).userId();
        return ApiResponse.ok(llmProviderService.test(ownerId, providerId, request));
    }
}
