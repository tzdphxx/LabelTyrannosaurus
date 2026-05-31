package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchAssignRequest;
import com.labelhub.modules.review.dto.BatchMarkManualRequest;
import com.labelhub.modules.review.dto.BatchRejectRequest;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.service.BatchReviewService;
import com.labelhub.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/submissions")
@Tag(name = "审核", description = "提交审核和批量审核")
public class ReviewController {

    private final ReviewService reviewService;
    private final BatchReviewService batchReviewService;

    public ReviewController(ReviewService reviewService,
                            BatchReviewService batchReviewService) {
        this.reviewService = reviewService;
        this.batchReviewService = batchReviewService;
    }

    @GetMapping
    @Operation(summary = "待终审提交列表", description = "查询 REVIEWER 可处理的待终审提交。")
    public ApiResponse<List<SubmissionReviewItem>> listPendingFinal() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewService.listPendingFinal());
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

    @PostMapping("/batch/reject")
    @Operation(summary = "批量驳回", description = "批量审核驳回提交。")
    public ApiResponse<BatchReviewResponse> batchReject(@Valid @RequestBody BatchRejectRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchReject(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/mark-manual")
    @Operation(summary = "批量转人工", description = "将提交批量标记为需要人工处理。")
    public ApiResponse<BatchReviewResponse> batchMarkManual(@Valid @RequestBody BatchMarkManualRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchMarkManual(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/assign")
    @Operation(summary = "批量分配审核", description = "批量分配提交给审核员。")
    public ApiResponse<BatchReviewResponse> batchAssign(@Valid @RequestBody BatchAssignRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchAssign(
                CurrentUserContext.getUserId(), request));
    }
}
