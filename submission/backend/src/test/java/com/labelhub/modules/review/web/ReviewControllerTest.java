package com.labelhub.modules.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.BatchReviewItemResult;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.dto.ReviewerSubmissionListItem;
import com.labelhub.modules.review.dto.ReviewerReviewTaskListItem;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.review.service.BatchReviewService;
import com.labelhub.modules.review.service.ReviewService;
import com.labelhub.modules.review.service.ReviewerSubmissionQueryService;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @Mock private BatchReviewService batchReviewService;
    @Mock private ReviewerSubmissionQueryService reviewerQueryService;
    @Mock private ReviewerSubmissionListMapper reviewerListMapper;

    private ReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewController(reviewService, batchReviewService,
                reviewerQueryService, reviewerListMapper);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void listDelegatesToMapper() {
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
        ReviewerSubmissionListItem item = new ReviewerSubmissionListItem(
                100L, 1L, 1L, 1L, SubmissionStatus.PENDING_FINAL, null, null, null, 1, null, null, null);
        when(reviewerListMapper.countWithFilters(null, null, null, null, null, null, null, 1L, null))
                .thenReturn(1L);
        when(reviewerListMapper.selectWithFilters(null, null, null, null, null, null, null, 1L, null, 0, 20))
                .thenReturn(List.of(item));

        ApiResponse<PageResponse<ReviewerSubmissionListItem>> response = controller.list(
                null, null, null, null, null, null, null, 1, 20);

        assertThat(response.data().items()).containsExactly(item);
        assertThat(response.data().total()).isEqualTo(1L);
    }

    @Test
    void reviewTasksDelegatesToMapperWithCurrentReviewerAndScope() {
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
        ReviewerReviewTaskListItem item = new ReviewerReviewTaskListItem(
                10L, "Risk review", "PUBLISHED", null,
                3, 1, 0,
                "UNCLAIMED", true, false, false);
        when(reviewerListMapper.selectReviewTasksForReviewer(1L, "UNCLAIMED"))
                .thenReturn(List.of(item));

        ApiResponse<List<ReviewerReviewTaskListItem>> response = controller.reviewTasks("UNCLAIMED");

        assertThat(response.data()).containsExactly(item);
    }

    @Test
    void routeMappingsKeepSubmissionPathsAndExposeReviewTaskMarketplace() throws NoSuchMethodException {
        RequestMapping classMapping = ReviewController.class.getAnnotation(RequestMapping.class);
        assertThat(classMapping.value()).containsExactly("/api/v1/reviewer");

        GetMapping reviewTasksMapping = ReviewController.class
                .getMethod("reviewTasks", String.class)
                .getAnnotation(GetMapping.class);
        assertThat(reviewTasksMapping.value()).containsExactly("/review-tasks");

        GetMapping submissionsMapping = ReviewController.class
                .getMethod("list", Long.class, String.class, String.class, String.class,
                        String.class, Integer.class, String.class, int.class, int.class)
                .getAnnotation(GetMapping.class);
        assertThat(submissionsMapping.value()).containsExactly("/submissions");
    }

    @Test
    void approvePassesCurrentUserAndReturnsResponse() {
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
        ReviewActionResponse serviceResponse = new ReviewActionResponse(
                100L, SubmissionStatus.APPROVED, 200L);
        when(reviewService.approve(eq(100L), eq(1L), any(ApproveRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<ReviewActionResponse> response = controller.approve(
                100L, new ApproveRequest("Looks good", 1, null));

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
    void adminCanReadReviewerSubmissionDetail() {
        CurrentUserContext.set(new CurrentUser(9L, "admin", "admin@labelhub.dev", Set.of(RoleCode.ADMIN), 1));
        ReviewerSubmissionDetailResponse serviceResponse = new ReviewerSubmissionDetailResponse(
                100L, 10L, 20L, 30L, 40L, 1, SubmissionStatus.PENDING_FINAL,
                "{}", "{}", 50L, "{}", null, null, List.of(), List.of(), null);
        when(reviewerQueryService.getDetail(100L)).thenReturn(serviceResponse);

        ApiResponse<ReviewerSubmissionDetailResponse> response = controller.getDetail(100L);

        assertThat(response.data()).isEqualTo(serviceResponse);
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

    @Test
    void contractBatchApproveAliasDelegatesToBatchService() {
        CurrentUserContext.set(new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1));
        BatchReviewResponse serviceResponse = new BatchReviewResponse(
                1, 1, 0, List.of(BatchReviewItemResult.ok(100L)));
        when(batchReviewService.batchApprove(eq(1L), any(BatchApproveRequest.class)))
                .thenReturn(serviceResponse);

        ApiResponse<BatchReviewResponse> response = controller.batchApproveAlias(
                new BatchApproveRequest(List.of(100L), "ok", 1));

        assertThat(response.data().successCount()).isEqualTo(1);
    }

    @Test
    void labelerCannotList() {
        CurrentUserContext.set(new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.list(null, null, null, null, null, null, null, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void labelerCannotListReviewTasks() {
        CurrentUserContext.set(new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.reviewTasks("ALL"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void labelerCannotApproveSubmission() {
        CurrentUserContext.set(new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.approve(100L, new ApproveRequest("ok", 1, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void labelerCannotBatchApprove() {
        CurrentUserContext.set(new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.batchApprove(new BatchApproveRequest(List.of(100L), "ok", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }
}
