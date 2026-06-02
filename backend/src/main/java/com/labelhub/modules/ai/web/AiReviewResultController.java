package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.service.AiReviewResultQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions/{submissionId}/ai-review-result")
@Tag(name = "AI 审核", description = "提交的 AI 审核结果查询")
public class AiReviewResultController {

    private final AiReviewResultQueryService queryService;

    public AiReviewResultController(AiReviewResultQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "AI 审核结果", description = "查询指定提交的 AI 审核结果。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AiReviewResultResponse> get(@PathVariable Long submissionId) {
        return ApiResponse.ok(queryService.getForSubmission(CurrentUserContext.requireCurrentUser(), submissionId));
    }
}
