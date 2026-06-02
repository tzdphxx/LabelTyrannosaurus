package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.OwnerModelOptionResponse;
import com.labelhub.modules.ai.service.LlmProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm-providers")
@Tag(name = "大模型选项", description = "OWNER 查看 Admin 启用的模型列表，从中选择模型配置任务 AI 审核")
public class LlmProviderController {

    private final LlmProviderService llmProviderService;

    public LlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @GetMapping
    @Operation(summary = "启用模型列表", description = "查询 Admin 启用的模型选项列表（仅包含选择和展示必要字段）。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<OwnerModelOptionResponse>> list() {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(llmProviderService.listEnabledForOwner());
    }
}
