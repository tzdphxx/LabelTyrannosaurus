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
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionListItem;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.review.service.BatchReviewService;
import com.labelhub.modules.review.service.ReviewService;
import com.labelhub.modules.review.service.ReviewerSubmissionQueryService;
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
    public ApiResponse<List<ReviewerSubmissionListItem>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String submissionStatus,
            @RequestParam(required = false) String aiDecision,
            @RequestParam(required = false) String aiReviewStatus,
            @RequestParam(required = false) String conflictStatus,
            @RequestParam(required = false) Integer reviewLevel,
            @RequestParam(required = false) Long assignedReviewerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        return ApiResponse.ok(reviewerListMapper.selectWithFilters(
                taskId, submissionStatus, aiDecision, aiReviewStatus,
                conflictStatus, reviewLevel, assignedReviewerId, offset, safeSize));
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<ReviewerSubmissionDetailResponse> getDetail(@PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewerQueryService.getDetail(submissionId));
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ReviewActionResponse> approve(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ApproveRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewService.approve(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ReviewActionResponse> reject(@PathVariable Long submissionId,
                                                     @Valid @RequestBody RejectRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(reviewService.reject(
                submissionId, CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/approve")
    public ApiResponse<BatchReviewResponse> batchApprove(@Valid @RequestBody BatchApproveRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchApprove(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/reject")
    public ApiResponse<BatchReviewResponse> batchReject(@Valid @RequestBody BatchRejectRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchReject(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/mark-manual")
    public ApiResponse<BatchReviewResponse> batchMarkManual(@Valid @RequestBody BatchMarkManualRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchMarkManual(
                CurrentUserContext.getUserId(), request));
    }

    @PostMapping("/batch/assign")
    public ApiResponse<BatchReviewResponse> batchAssign(@Valid @RequestBody BatchAssignRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(batchReviewService.batchAssign(
                CurrentUserContext.getUserId(), request));
    }
}
