package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.BatchApproveRequest;
import com.labelhub.modules.review.dto.BatchAssignRequest;
import com.labelhub.modules.review.dto.BatchMarkManualRequest;
import com.labelhub.modules.review.dto.BatchRejectRequest;
import com.labelhub.modules.review.dto.BatchReviewResponse;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchReviewServiceTest {

    private static final Long REVIEWER_ID = 1L;

    @Mock private ReviewService reviewService;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;

    private BatchReviewService batchReviewService;

    @BeforeEach
    void setUp() {
        batchReviewService = new BatchReviewService(
                reviewService, submissionMapper, reviewRecordMapper);
    }

    @Test
    void batchApproveSucceedsForNonConflict() {
        Submission s = pendingFinalSubmission(100L, 1L, 1L);
        when(submissionMapper.selectById(100L)).thenReturn(s);
        when(submissionMapper.countPendingFinalByTaskAndItem(1L, 1L)).thenReturn(1);
        when(reviewService.approve(eq(100L), eq(REVIEWER_ID), any(ApproveRequest.class)))
                .thenReturn(new ReviewActionResponse(100L, SubmissionStatus.APPROVED, 10L));

        BatchReviewResponse response = batchReviewService.batchApprove(
                REVIEWER_ID, new BatchApproveRequest(List.of(100L), "ok", 1));

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failCount()).isEqualTo(0);
    }

    @Test
    void batchApproveRejectsConflictSubmission() {
        Submission s = pendingFinalSubmission(100L, 1L, 1L);
        when(submissionMapper.selectById(100L)).thenReturn(s);
        when(submissionMapper.countPendingFinalByTaskAndItem(1L, 1L)).thenReturn(2);

        BatchReviewResponse response = batchReviewService.batchApprove(
                REVIEWER_ID, new BatchApproveRequest(List.of(100L), "ok", 1));

        assertThat(response.failCount()).isEqualTo(1);
        assertThat(response.results().get(0).error())
                .contains("Conflict");
        verify(reviewService, never()).approve(any(), any(), any());
    }

    @Test
    void batchApprovePartialSuccess() {
        Submission s1 = pendingFinalSubmission(100L, 1L, 1L);
        Submission s2 = pendingFinalSubmission(101L, 1L, 2L);
        when(submissionMapper.selectById(100L)).thenReturn(s1);
        when(submissionMapper.selectById(101L)).thenReturn(s2);
        when(submissionMapper.countPendingFinalByTaskAndItem(1L, 1L)).thenReturn(1);
        when(submissionMapper.countPendingFinalByTaskAndItem(1L, 2L)).thenReturn(1);
        when(reviewService.approve(eq(100L), eq(REVIEWER_ID), any()))
                .thenReturn(new ReviewActionResponse(100L, SubmissionStatus.APPROVED, 10L));
        when(reviewService.approve(eq(101L), eq(REVIEWER_ID), any()))
                .thenThrow(new BusinessException(400601, "Not reviewable"));

        BatchReviewResponse response = batchReviewService.batchApprove(
                REVIEWER_ID, new BatchApproveRequest(List.of(100L, 101L), "ok", 1));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failCount()).isEqualTo(1);
        assertThat(response.results().get(0).success()).isTrue();
        assertThat(response.results().get(1).success()).isFalse();
    }

    @Test
    void batchRejectSucceeds() {
        when(reviewService.reject(eq(100L), eq(REVIEWER_ID), any()))
                .thenReturn(new ReviewActionResponse(100L, SubmissionStatus.REJECTED, 10L));

        BatchReviewResponse response = batchReviewService.batchReject(
                REVIEWER_ID, new BatchRejectRequest(List.of(100L), "Bad quality", 1));

        assertThat(response.successCount()).isEqualTo(1);
    }

    @Test
    void batchMarkManualSucceeds() {
        Submission s = pendingFinalSubmission(100L, 1L, 1L);
        when(submissionMapper.selectById(100L)).thenReturn(s);

        BatchReviewResponse response = batchReviewService.batchMarkManual(
                REVIEWER_ID, new BatchMarkManualRequest(List.of(100L)));

        assertThat(response.successCount()).isEqualTo(1);
        verify(reviewRecordMapper).insert(any(ReviewRecord.class));
    }

    @Test
    void batchAssignSucceeds() {
        Submission s = pendingFinalSubmission(100L, 1L, 1L);
        when(submissionMapper.selectById(100L)).thenReturn(s);

        BatchReviewResponse response = batchReviewService.batchAssign(
                REVIEWER_ID, new BatchAssignRequest(List.of(100L), 2L));

        assertThat(response.successCount()).isEqualTo(1);
        verify(reviewRecordMapper).insert(any(ReviewRecord.class));
    }

    @Test
    void batchApproveNotFoundReturnsFailure() {
        when(submissionMapper.selectById(999L)).thenReturn(null);

        BatchReviewResponse response = batchReviewService.batchApprove(
                REVIEWER_ID, new BatchApproveRequest(List.of(999L), "ok", 1));

        assertThat(response.failCount()).isEqualTo(1);
        assertThat(response.results().get(0).error()).contains("not found");
    }

    private Submission pendingFinalSubmission(Long id, Long taskId, Long datasetItemId) {
        Submission s = new Submission();
        s.setId(id);
        s.setTaskId(taskId);
        s.setDatasetItemId(datasetItemId);
        s.setAssignmentId(10L);
        s.setStatus(SubmissionStatus.PENDING_FINAL);
        return s;
    }
}
