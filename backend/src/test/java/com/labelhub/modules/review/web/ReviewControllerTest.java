package com.labelhub.modules.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.BatchReviewItemResult;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.service.BatchReviewService;
import com.labelhub.modules.review.service.ReviewService;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @Mock private BatchReviewService batchReviewService;
    @Mock private CurrentUserContext currentUserContext;

    private ReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewController(reviewService, batchReviewService, currentUserContext);
    }

    @Test
    void listPendingFinalDelegatesToService() {
        SubmissionReviewItem item = new SubmissionReviewItem(
                100L, 1L, 1L, 1L, SubmissionStatus.PENDING_FINAL, null, null, 1);
        when(reviewService.listPendingFinal()).thenReturn(List.of(item));

        ApiResponse<List<SubmissionReviewItem>> response = controller.listPendingFinal();

        assertThat(response.data()).containsExactly(item);
    }

    @Test
    void approvePassesCurrentUserAndReturnsResponse() {
        when(currentUserContext.currentUserId()).thenReturn(1L);
        ReviewActionResponse serviceResponse = new ReviewActionResponse(
                100L, SubmissionStatus.APPROVED, 200L);
        when(reviewService.approve(eq(100L), eq(1L), any(ApproveRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<ReviewActionResponse> response = controller.approve(
                100L, new ApproveRequest("Looks good", 1));

        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().submissionStatus()).isEqualTo(SubmissionStatus.APPROVED);
    }

    @Test
    void rejectPassesCurrentUserAndReturnsResponse() {
        when(currentUserContext.currentUserId()).thenReturn(1L);
        ReviewActionResponse serviceResponse = new ReviewActionResponse(
                100L, SubmissionStatus.REJECTED, 201L);
        when(reviewService.reject(eq(100L), eq(1L), any(RejectRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<ReviewActionResponse> response = controller.reject(
                100L, new RejectRequest("Missing label", 1));

        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().submissionStatus()).isEqualTo(SubmissionStatus.REJECTED);
    }

    @Test
    void batchApproveDelegatesToBatchService() {
        when(currentUserContext.currentUserId()).thenReturn(1L);
        BatchReviewResponse serviceResponse = new BatchReviewResponse(
                1, 1, 0, List.of(BatchReviewItemResult.ok(100L)));
        when(batchReviewService.batchApprove(eq(1L), any(BatchApproveRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<BatchReviewResponse> response = controller.batchApprove(
                new BatchApproveRequest(List.of(100L), "ok", 1));

        assertThat(response.data().successCount()).isEqualTo(1);
    }
}
