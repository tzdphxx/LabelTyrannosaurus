package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchMarkManualRequest;
import com.labelhub.modules.review.dto.BatchRejectRequest;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionListItem;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.review.service.BatchReviewService;
import com.labelhub.modules.review.service.ReviewService;
import com.labelhub.modules.review.service.ReviewerSubmissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/submissions")
@Tag(name = "审核", description = "提交审核和批量审核")
public class ReviewController {

    private final ReviewService reviewService;
    private final BatchReviewService batchReviewService;
    private final ReviewerSubmissionQueryService reviewerQueryService;
    private final ReviewerSubmissionListMapper reviewerListMapper;

    public ReviewController(ReviewService reviewService,
                            BatchReviewService batchReviewService,
                            ReviewerSubmissionQueryService reviewerQueryService,
                            ReviewerSubmissionListMapper reviewerListMapper) {
        this.reviewService = reviewService;
        this.batchReviewService = batchReviewService;
        this.reviewerQueryService = reviewerQueryService;
        this.reviewerListMapper = reviewerListMapper;
    }

    @GetMapping
    @Operation(summary = "待审提交列表", description = "查询审核员可处理的提交列表，支持按任务、提交状态、AI 结论、冲突状态、审核级别和分配审核员筛选。")
    public ApiResponse<PageResponse<ReviewerSubmissionListItem>> list(
            @Parameter(description = "按任务 ID 筛选") @RequestParam(required = false) Long taskId,
            @Parameter(description = "按提交状态筛选") @RequestParam(required = false) String submissionStatus,
            @Parameter(description = "按 AI 结论筛选：PASS / REJECT / MANUAL_REVIEW") @RequestParam(required = false) String aiDecision,
            @Parameter(description = "按 AI 审核状态筛选") @RequestParam(required = false) String aiReviewStatus,
            @Parameter(description = "按冲突状态筛选") @RequestParam(required = false) String conflictStatus,
            @Parameter(description = "按审核级别筛选") @RequestParam(required = false) Integer reviewLevel,
            @Parameter(description = "按分配的审核员 ID 筛选") @RequestParam(required = false) Long assignedReviewerId,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 20，最大 100") @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        long total = reviewerListMapper.countWithFilters(
                taskId, submissionStatus, aiDecision, aiReviewStatus,
                conflictStatus, reviewLevel, assignedReviewerId);
        List<ReviewerSubmissionListItem> items = reviewerListMapper.selectWithFilters(
                taskId, submissionStatus, aiDecision, aiReviewStatus,
                conflictStatus, reviewLevel, assignedReviewerId, offset, safeSize);
        return ApiResponse.ok(new PageResponse<>(items, safePage, safeSize, total));
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "提交审核详情", description = "查询指定提交的审核详情，包含标注答案、AI 评分、审核历史、冲突信息等。")
    public ApiResponse<ReviewerSubmissionDetailResponse> getDetail(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewerQueryService.getDetail(submissionId));
    }

    @PostMapping("/{submissionId}/approve")
    @Operation(summary = "通过提交", description = "审核通过指定提交。")
    public ApiResponse<ReviewActionResponse> approve(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ApproveRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewService.approve(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/{submissionId}/reject")
    @Operation(summary = "驳回提交", description = "审核驳回指定提交。")
    public ApiResponse<ReviewActionResponse> reject(@PathVariable Long submissionId,
                                                     @Valid @RequestBody RejectRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewService.reject(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/approve")
    @Operation(summary = "批量通过", description = "批量审核通过提交。")
    public ApiResponse<BatchReviewResponse> batchApprove(@Valid @RequestBody BatchApproveRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchApprove(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量通过", description = "兼容契约路径，批量审核通过提交。")
    public ApiResponse<BatchReviewResponse> batchApproveAlias(@Valid @RequestBody BatchApproveRequest request) {
        return batchApprove(request);
    }

    @PostMapping("/batch/reject")
    @Operation(summary = "批量驳回", description = "批量审核驳回提交。")
    public ApiResponse<BatchReviewResponse> batchReject(@Valid @RequestBody BatchRejectRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchReject(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch-reject")
    @Operation(summary = "批量驳回", description = "兼容契约路径，批量审核驳回提交。")
    public ApiResponse<BatchReviewResponse> batchRejectAlias(@Valid @RequestBody BatchRejectRequest request) {
        return batchReject(request);
    }

    @PostMapping("/batch/mark-manual")
    @Operation(summary = "批量转人工", description = "将提交批量标记为需要人工处理。")
    public ApiResponse<BatchReviewResponse> batchMarkManual(@Valid @RequestBody BatchMarkManualRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchMarkManual(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch-mark-manual")
    @Operation(summary = "批量转人工", description = "兼容契约路径，将提交批量标记为需要人工处理。")
    public ApiResponse<BatchReviewResponse> batchMarkManualAlias(@Valid @RequestBody BatchMarkManualRequest request) {
        return batchMarkManual(request);
    }
}
