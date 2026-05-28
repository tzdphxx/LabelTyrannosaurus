package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
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
public class ReviewController {

    private final ReviewService reviewService;
    private final BatchReviewService batchReviewService;

    public ReviewController(ReviewService reviewService,
                            BatchReviewService batchReviewService) {
        this.reviewService = reviewService;
        this.batchReviewService = batchReviewService;
    }

    @GetMapping
    public ApiResponse<List<SubmissionReviewItem>> listPendingFinal() {
        return ApiResponse.ok(reviewService.listPendingFinal());
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ReviewActionResponse> approve(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(reviewService.approve(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ReviewActionResponse> reject(@PathVariable Long submissionId,
                                                     @Valid @RequestBody RejectRequest request) {
        return ApiResponse.ok(reviewService.reject(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/approve")
    public ApiResponse<BatchReviewResponse> batchApprove(@Valid @RequestBody BatchApproveRequest request) {
        return ApiResponse.ok(batchReviewService.batchApprove(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/reject")
    public ApiResponse<BatchReviewResponse> batchReject(@Valid @RequestBody BatchRejectRequest request) {
        return ApiResponse.ok(batchReviewService.batchReject(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/mark-manual")
    public ApiResponse<BatchReviewResponse> batchMarkManual(@Valid @RequestBody BatchMarkManualRequest request) {
        return ApiResponse.ok(batchReviewService.batchMarkManual(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/assign")
    public ApiResponse<BatchReviewResponse> batchAssign(@Valid @RequestBody BatchAssignRequest request) {
        return ApiResponse.ok(batchReviewService.batchAssign(
                CurrentUserContext.getUserId(), request));
    }
}
