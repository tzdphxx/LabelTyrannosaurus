package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.service.LlmTriggerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm/triggers")
public class LlmTriggerController {

    private final LlmTriggerService llmTriggerService;
    private final CurrentUserContext currentUserContext;

    public LlmTriggerController(LlmTriggerService llmTriggerService, CurrentUserContext currentUserContext) {
        this.llmTriggerService = llmTriggerService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping("/run")
    public ApiResponse<LlmTriggerRunResponse> run(@Valid @RequestBody LlmTriggerRunRequest request) {
        return ApiResponse.ok(llmTriggerService.run(currentUserContext.currentUser(), request));
    }
}
