package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.service.AiReviewManualRetryService;
import com.labelhub.modules.ai.service.AiReviewResultQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions/{submissionId}/ai-review")
@Tag(name = "AI 审核结果", description = "查询和重试 AI 自动预审结果")
public class AiReviewController {

    private final AiReviewResultQueryService queryService;
    private final AiReviewManualRetryService manualRetryService;

    public AiReviewController(AiReviewResultQueryService queryService,
                              AiReviewManualRetryService manualRetryService) {
        this.queryService = queryService;
        this.manualRetryService = manualRetryService;
    }

    @GetMapping
    @Operation(summary = "查询 AI 审核结果", description = "获取指定提交的 AI 预审结果，包含各维度评分、结论、置信度、风险标记和原始 Prompt/响应。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AiReviewResultResponse> get(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        return ApiResponse.ok(queryService.getForSubmission(
                CurrentUserContext.requireCurrentUser(), submissionId));
    }

    @PostMapping("/retry")
    @Operation(summary = "手动重试 AI 审核", description = "审核员手动触发 AI 预审重试，适用于 AI 审核失败或需要重新评估的场景。每次重试产生新的 AgentRun 记录。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AiReviewResultResponse> retry(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(manualRetryService.retry(
                submissionId, CurrentUserContext.getUserId()));
    }
}
