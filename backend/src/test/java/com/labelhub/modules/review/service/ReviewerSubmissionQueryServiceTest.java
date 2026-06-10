package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewTaskClaimMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReviewerSubmissionQueryServiceTest {

    private final SubmissionMapper submissionMapper = org.mockito.Mockito.mock(SubmissionMapper.class);
    private final ReviewTaskClaimMapper reviewTaskClaimMapper = org.mockito.Mockito.mock(ReviewTaskClaimMapper.class);
    private final ReviewerSubmissionQueryService service = new ReviewerSubmissionQueryService(
            submissionMapper,
            org.mockito.Mockito.mock(AssignmentMapper.class),
            org.mockito.Mockito.mock(DatasetItemMapper.class),
            org.mockito.Mockito.mock(TemplateVersionMapper.class),
            org.mockito.Mockito.mock(AiReviewResultMapper.class),
            org.mockito.Mockito.mock(AgentRunMapper.class),
            org.mockito.Mockito.mock(ReviewRecordMapper.class),
            org.mockito.Mockito.mock(PreAnnotationMapper.class),
            reviewTaskClaimMapper);

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void reviewerCannotReadUnassignedSubmissionDetailById() {
        CurrentUserContext.set(new CurrentUser(30L, "reviewer", "r@test.dev", Set.of(RoleCode.REVIEWER), 1));
        when(submissionMapper.selectById(10L)).thenReturn(submission(10L, 99L));

        assertThatThrownBy(() -> service.getDetail(10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403601));
    }

    @Test
    void assignedReviewerCanReadSubmissionDetail() {
        CurrentUserContext.set(new CurrentUser(30L, "reviewer", "r@test.dev", Set.of(RoleCode.REVIEWER), 1));
        when(submissionMapper.selectById(10L)).thenReturn(submission(10L, 30L));

        assertThat(service.getDetail(10L).submissionId()).isEqualTo(10L);
    }

    @Test
    void taskLevelClaimantCanReadSubmissionDetailBeforeAssignment() {
        CurrentUserContext.set(new CurrentUser(30L, "reviewer", "r@test.dev", Set.of(RoleCode.REVIEWER), 1));
        Submission submission = submission(10L, null);
        submission.setStatus(SubmissionStatus.AI_REVIEWING);
        submission.setCurrentReviewLevel(1);
        when(submissionMapper.selectById(10L)).thenReturn(submission);
        when(reviewTaskClaimMapper.selectReviewerForTaskLevel(20L, 1)).thenReturn(30L);

        assertThat(service.getDetail(10L).submissionId()).isEqualTo(10L);
    }

    private Submission submission(Long id, Long assignedReviewerId) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setTaskId(20L);
        submission.setAssignmentId(40L);
        submission.setDatasetItemId(50L);
        submission.setLabelerId(60L);
        submission.setVersionNo(1);
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submission.setAssignedReviewerId(assignedReviewerId);
        return submission;
    }
}
