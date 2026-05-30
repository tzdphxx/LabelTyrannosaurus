package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.service.AiReviewManualRetryService;
import com.labelhub.modules.ai.service.AiReviewResultQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions/{submissionId}/ai-review")
public class AiReviewController {

    private final AiReviewResultQueryService queryService;
    private final AiReviewManualRetryService manualRetryService;

    public AiReviewController(AiReviewResultQueryService queryService,
                              AiReviewManualRetryService manualRetryService) {
        this.queryService = queryService;
        this.manualRetryService = manualRetryService;
    }

    @GetMapping
    public ApiResponse<AiReviewResultResponse> get(@PathVariable Long submissionId) {
        return ApiResponse.ok(queryService.getForSubmission(
                CurrentUserContext.requireCurrentUser(), submissionId));
    }

    @PostMapping("/retry")
    public ApiResponse<AiReviewResultResponse> retry(@PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(manualRetryService.retry(
                submissionId, CurrentUserContext.getUserId()));
    }
}
