package com.labelhub.modules.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
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
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @Mock private BatchReviewService batchReviewService;

    private ReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewController(reviewService, batchReviewService);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
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
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
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
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
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
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
        BatchReviewResponse serviceResponse = new BatchReviewResponse(
                1, 1, 0, List.of(BatchReviewItemResult.ok(100L)));
        when(batchReviewService.batchApprove(eq(1L), any(BatchApproveRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<BatchReviewResponse> response = controller.batchApprove(
                new BatchApproveRequest(List.of(100L), "ok", 1));

        assertThat(response.data().successCount()).isEqualTo(1);
    }
}
