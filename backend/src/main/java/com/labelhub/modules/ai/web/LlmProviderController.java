package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.ai.dto.CreateLlmProviderRequest;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.dto.TestLlmProviderRequest;
import com.labelhub.modules.ai.dto.UpdateLlmProviderRequest;
import com.labelhub.modules.ai.service.LlmProviderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/llm-providers")
public class LlmProviderController {

    private final LlmProviderService llmProviderService;

    public LlmProviderController(LlmProviderService llmProviderService) {
        this.llmProviderService = llmProviderService;
    }

    @GetMapping
    public ApiResponse<List<LlmProviderResponse>> list() {
        return ApiResponse.ok(llmProviderService.list());
    }

    @PostMapping
    public ApiResponse<LlmProviderResponse> create(@RequestHeader("X-User-Id") Long actorId,
                                                   @Valid @RequestBody CreateLlmProviderRequest request) {
        return ApiResponse.ok(llmProviderService.create(actorId, request));
    }

    @PutMapping("/{providerId}")
    public ApiResponse<LlmProviderResponse> update(@RequestHeader("X-User-Id") Long actorId,
                                                   @PathVariable Long providerId,
                                                   @Valid @RequestBody UpdateLlmProviderRequest request) {
        return ApiResponse.ok(llmProviderService.update(actorId, providerId, request));
    }

    @PostMapping("/{providerId}/enable")
    public ApiResponse<LlmProviderResponse> enable(@RequestHeader("X-User-Id") Long actorId,
                                                   @PathVariable Long providerId) {
        return ApiResponse.ok(llmProviderService.enable(actorId, providerId));
    }

    @PostMapping("/{providerId}/disable")
    public ApiResponse<LlmProviderResponse> disable(@RequestHeader("X-User-Id") Long actorId,
                                                    @PathVariable Long providerId) {
        return ApiResponse.ok(llmProviderService.disable(actorId, providerId));
    }

    @PostMapping("/{providerId}/test")
    public ApiResponse<LlmProviderTestResponse> test(@PathVariable Long providerId,
                                                     @Valid @RequestBody TestLlmProviderRequest request) {
        return ApiResponse.ok(llmProviderService.test(providerId, request));
    }
}
