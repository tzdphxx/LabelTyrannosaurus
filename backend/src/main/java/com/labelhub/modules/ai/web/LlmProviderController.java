package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.service.LlmProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm-providers")
@Tag(name = "LLM 厂商", description = "OWNER 查询自己可用的 LLM 厂商和模型列表")
public class LlmProviderController {

    private final LlmProviderService llmProviderService;

    public LlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @GetMapping
    @Operation(summary = "可用模型供应商列表", description = "Owner 查询 ADMIN 已启用的 LLM Provider，仅返回前端可展示的安全配置信息。")
    public ApiResponse<List<LlmProviderResponse>> list() {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(llmProviderService.listEnabled());
    }
}
