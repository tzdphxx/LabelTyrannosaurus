package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.AiReviewPromptTestRequest;
import com.labelhub.modules.ai.dto.AiReviewPromptTestResponse;
import com.labelhub.modules.ai.service.AiReviewConfigService;
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
public class AiReviewConfigController {

    private final AiReviewConfigService aiReviewConfigService;

    public AiReviewConfigController(AiReviewConfigService aiReviewConfigService) {
        this.aiReviewConfigService = aiReviewConfigService;
    }

    @PostMapping
    public ApiResponse<AiReviewConfigResponse> save(@PathVariable Long taskId,
                                                    @Valid @RequestBody AiReviewConfigRequest request) {
        return ApiResponse.ok(aiReviewConfigService.save(CurrentUserContext.getUserId(), taskId, request));
    }

    @PutMapping("/{configId}")
    public ApiResponse<AiReviewConfigResponse> update(@PathVariable Long taskId,
                                                      @PathVariable Long configId,
                                                      @Valid @RequestBody AiReviewConfigRequest request) {
        return ApiResponse.ok(aiReviewConfigService.update(CurrentUserContext.getUserId(), taskId, configId,
                request));
    }

    @GetMapping
    public ApiResponse<AiReviewConfigResponse> get(@PathVariable Long taskId) {
        return ApiResponse.ok(aiReviewConfigService.get(CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{configId}/test")
    public ApiResponse<AiReviewPromptTestResponse> test(@PathVariable Long taskId,
                                                        @PathVariable Long configId,
                                                        @Valid @RequestBody AiReviewPromptTestRequest request) {
        return ApiResponse.ok(aiReviewConfigService.testPrompt(CurrentUserContext.getUserId(), taskId, configId,
                request));
    }
}
