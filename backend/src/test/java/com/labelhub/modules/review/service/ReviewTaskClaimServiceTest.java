package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.review.domain.ReviewTaskClaim;
import com.labelhub.modules.review.dto.ReviewTaskClaimResponse;
import com.labelhub.modules.review.mapper.ReviewTaskClaimMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewTaskClaimServiceTest {

    private static final Long REVIEWER_A = 1L;
    private static final Long REVIEWER_B = 2L;
    private static final Long TASK_ID = 10L;

    @Mock private ReviewTaskClaimMapper claimMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private AuditAppender auditAppender;
    @Mock private TraceIdProvider traceIdProvider;

    private ReviewTaskClaimService service;

    @BeforeEach
    void setUp() {
        lenient().when(traceIdProvider.currentTraceId()).thenReturn("trace-review-claim");
        service = new ReviewTaskClaimService(claimMapper, submissionMapper, taskMapper, auditAppender, traceIdProvider);
    }

    @Test
    void claimAssignsAllPendingSubmissionsAndInsertsClaim() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(2));
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(null);
        when(claimMapper.selectByTask(TASK_ID)).thenReturn(List.of());
        when(submissionMapper.assignReviewerForTaskLevel(TASK_ID, 1, REVIEWER_A)).thenReturn(7);

        ReviewTaskClaimResponse response = service.claim(REVIEWER_A, TASK_ID, 1);

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.reviewLevel()).isEqualTo(1);
        assertThat(response.claimedSubmissionCount()).isEqualTo(7);
        verify(claimMapper).insert(any(ReviewTaskClaim.class));
        ArgumentCaptor<AuditCommand> auditCaptor = ArgumentCaptor.forClass(AuditCommand.class);
        verify(auditAppender).append(auditCaptor.capture());
        assertThat(auditCaptor.getValue().traceId()).isEqualTo("trace-review-claim");
    }

    @Test
    void claimByOtherReviewerThrowsConflict() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(1));
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(claim(REVIEWER_B, 1));

        assertThatThrownBy(() -> service.claim(REVIEWER_A, TASK_ID, 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409201));
        verify(claimMapper, never()).insert(any(ReviewTaskClaim.class));
    }

    @Test
    void claimSameReviewerIsIdempotentAndReassigns() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(1));
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(claim(REVIEWER_A, 1));
        when(submissionMapper.assignReviewerForTaskLevel(TASK_ID, 1, REVIEWER_A)).thenReturn(3);

        ReviewTaskClaimResponse response = service.claim(REVIEWER_A, TASK_ID, 1);

        assertThat(response.claimedSubmissionCount()).isEqualTo(3);
        verify(claimMapper, never()).insert(any(ReviewTaskClaim.class));
    }

    @Test
    void claimInvalidLevelThrows() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(1));

        assertThatThrownBy(() -> service.claim(REVIEWER_A, TASK_ID, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400603));
    }

    @Test
    void claimDraftTaskThrows() {
        Task draft = task(1);
        draft.setStatus(TaskStatus.DRAFT);
        when(taskMapper.selectById(TASK_ID)).thenReturn(draft);

        assertThatThrownBy(() -> service.claim(REVIEWER_A, TASK_ID, 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400601));
    }

    @Test
    void claimCrossLevelBySameReviewerThrows() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(2));
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 2)).thenReturn(null);
        when(claimMapper.selectByTask(TASK_ID)).thenReturn(List.of(claim(REVIEWER_A, 1)));

        assertThatThrownBy(() -> service.claim(REVIEWER_A, TASK_ID, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403601));
    }

    @Test
    void releaseClearsOwnershipAndDeletesClaim() {
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(claim(REVIEWER_A, 1));

        service.release(REVIEWER_A, TASK_ID, 1);

        verify(submissionMapper).clearReviewerForTaskLevel(TASK_ID, 1, REVIEWER_A);
        verify(claimMapper).deleteById(anyLong());
        ArgumentCaptor<AuditCommand> auditCaptor = ArgumentCaptor.forClass(AuditCommand.class);
        verify(auditAppender).append(auditCaptor.capture());
        assertThat(auditCaptor.getValue().traceId()).isEqualTo("trace-review-claim");
    }

    @Test
    void releaseByOtherReviewerThrows() {
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(claim(REVIEWER_B, 1));

        assertThatThrownBy(() -> service.release(REVIEWER_A, TASK_ID, 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409201));
        verify(claimMapper, never()).deleteById(anyLong());
    }

    @Test
    void releaseNonexistentClaimIsNoop() {
        when(claimMapper.selectByTaskAndLevel(TASK_ID, 1)).thenReturn(null);

        service.release(REVIEWER_A, TASK_ID, 1);

        verify(submissionMapper, never()).clearReviewerForTaskLevel(any(), any(), any());
    }

    private Task task(int reviewLevelCount) {
        Task t = new Task();
        t.setId(TASK_ID);
        t.setStatus(TaskStatus.PUBLISHED);
        t.setReviewLevelCount(reviewLevelCount);
        return t;
    }

    private ReviewTaskClaim claim(Long reviewerId, int level) {
        ReviewTaskClaim c = new ReviewTaskClaim();
        c.setId(99L);
        c.setTaskId(TASK_ID);
        c.setReviewLevel(level);
        c.setReviewerId(reviewerId);
        return c;
    }
}
