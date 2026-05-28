package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
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
    private final CurrentUserContext currentUserContext;

    public ReviewController(ReviewService reviewService, CurrentUserContext currentUserContext) {
        this.reviewService = reviewService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<List<SubmissionReviewItem>> listPendingFinal() {
        return ApiResponse.ok(reviewService.listPendingFinal());
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ReviewActionResponse> approve(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(reviewService.approve(
                submissionId, currentUserContext.currentUserId(), request));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ReviewActionResponse> reject(@PathVariable Long submissionId,
                                                     @Valid @RequestBody RejectRequest request) {
        return ApiResponse.ok(reviewService.reject(
                submissionId, currentUserContext.currentUserId(), request));
    }
}
