package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.service.AiReviewResultQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions/{submissionId}/ai-review-result")
public class AiReviewResultController {

    private final AiReviewResultQueryService queryService;
    private final CurrentUserContext currentUserContext;

    public AiReviewResultController(AiReviewResultQueryService queryService,
                                    CurrentUserContext currentUserContext) {
        this.queryService = queryService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<AiReviewResultResponse> get(@PathVariable Long submissionId) {
        return ApiResponse.ok(queryService.getForSubmission(currentUserContext.currentUser(), submissionId));
    }
}
