package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewSubmissionMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long REVIEWER_ID = 1L;
    private static final Long SUBMISSION_ID = 100L;
    private static final Long ASSIGNMENT_ID = 10L;

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private ReviewSubmissionMapper reviewSubmissionMapper;
    @Mock private SubmissionEventPublisher eventPublisher;
    @Mock private AuditAppender auditAppender;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                submissionMapper, assignmentMapper, reviewRecordMapper,
                reviewSubmissionMapper, eventPublisher, auditAppender);
    }

    // --- approve ---

    @Test
    void approveSetsStatusApprovedAndGolden() {
        Submission submission = pendingFinalSubmission();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        ReviewActionResponse response = reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("Looks good", 1));

        assertThat(response.submissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getIsGolden()).isTrue();
    }

    @Test
    void approveSetsAssignmentStatusApproved() {
        Assignment assignment = submittedAssignment();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(assignment);

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.APPROVED);
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void approveCreatesReviewRecordWithLevel() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("good", 1));

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(captor.capture());
        ReviewRecord record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(ReviewAction.APPROVE);
        assertThat(record.getReviewLevel()).isEqualTo(1);
        assertThat(record.getReviewerId()).isEqualTo(REVIEWER_ID);
        assertThat(record.getSubmissionId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void approvePublishesEvent() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        verify(eventPublisher).publishApproved(SUBMISSION_ID, REVIEWER_ID);
    }

    @Test
    void approveWritesAudit() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        verify(auditAppender).append(
                eq("SUBMISSION"), eq(SUBMISSION_ID),
                eq("USER"), eq(REVIEWER_ID),
                eq("SUBMISSION_APPROVED"),
                any(), any(), isNull(), isNull());
    }

    @Test
    void approveNonPendingFinalThrows() {
        Submission submission = pendingFinalSubmission();
        submission.setStatus(SubmissionStatus.APPROVED);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);

        assertThatThrownBy(() -> reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400601));
        verify(eventPublisher, never()).publishApproved(any(), any());
    }

    @Test
    void approveNotFoundThrows() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404601));
    }

    // --- reject ---

    @Test
    void rejectSetsStatusRejected() {
        Submission submission = pendingFinalSubmission();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        ReviewActionResponse response = reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Missing label on item 3", 1));

        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
    }

    @Test
    void rejectSetsAssignmentStatusReturned() {
        Assignment assignment = submittedAssignment();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(assignment);

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.RETURNED);
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void rejectCreatesReviewRecordWithReason() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(captor.capture());
        ReviewRecord record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(ReviewAction.REJECT);
        assertThat(record.getReason()).isEqualTo("Bad label");
        assertThat(record.getReviewLevel()).isEqualTo(1);
    }

    @Test
    void rejectBlankReasonThrows() {
        assertThatThrownBy(() -> reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400602));
        verify(submissionMapper, never()).selectById(any());
    }

    @Test
    void rejectNonPendingFinalThrows() {
        Submission submission = pendingFinalSubmission();
        submission.setStatus(SubmissionStatus.REJECTED);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);

        assertThatThrownBy(() -> reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("again", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400601));
    }

    @Test
    void rejectDoesNotPublishEvent() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        verify(eventPublisher, never()).publishApproved(any(), any());
    }

    // --- listPendingFinal ---

    @Test
    void listPendingFinalDelegatesMapper() {
        SubmissionReviewItem item = new SubmissionReviewItem(
                SUBMISSION_ID, 1L, 1L, 1L, SubmissionStatus.PENDING_FINAL, null, null, 1);
        when(reviewSubmissionMapper.selectPendingFinalItems()).thenReturn(List.of(item));

        List<SubmissionReviewItem> result = reviewService.listPendingFinal();

        assertThat(result).containsExactly(item);
    }

    // --- helpers ---

    private Submission pendingFinalSubmission() {
        Submission s = new Submission();
        s.setId(SUBMISSION_ID);
        s.setAssignmentId(ASSIGNMENT_ID);
        s.setStatus(SubmissionStatus.PENDING_FINAL);
        return s;
    }

    private Assignment submittedAssignment() {
        Assignment a = new Assignment();
        a.setId(ASSIGNMENT_ID);
        a.setStatus(AssignmentStatus.SUBMITTED);
        return a;
    }
}
