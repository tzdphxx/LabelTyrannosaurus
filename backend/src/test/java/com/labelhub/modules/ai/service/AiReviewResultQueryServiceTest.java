package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiReviewResultQueryServiceTest {

    private final AiReviewResultMapper aiReviewResultMapper = org.mockito.Mockito.mock(AiReviewResultMapper.class);
    private final SubmissionMapper submissionMapper = org.mockito.Mockito.mock(SubmissionMapper.class);
    private final TaskMapper taskMapper = org.mockito.Mockito.mock(TaskMapper.class);
    private final AiAutoReviewService aiAutoReviewService = org.mockito.Mockito.mock(AiAutoReviewService.class);
    private final AiReviewResultQueryService service = new AiReviewResultQueryService(
            aiReviewResultMapper, submissionMapper, taskMapper, aiAutoReviewService);

    @Test
    void reviewerCannotReadAiReviewForUnassignedSubmission() {
        Submission submission = submission(10L, 20L, 99L);
        when(submissionMapper.selectById(10L)).thenReturn(submission);
        when(aiReviewResultMapper.selectBySubmissionId(10L)).thenReturn(result(10L));

        assertThatThrownBy(() -> service.getForSubmission(reviewer(30L), 10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403703));
    }

    @Test
    void assignedReviewerCanReadAiReviewResult() {
        Submission submission = submission(10L, 20L, 30L);
        AiReviewResult result = result(10L);
        AiReviewResultResponse response = new AiReviewResultResponse(
                1L, 10L, 2L, 3L, "qwen", AiReviewStatus.SUCCESS, "PASS",
                "90.00", Map.of(), "[]", "ok", null, null, null, null, null, null);
        when(submissionMapper.selectById(10L)).thenReturn(submission);
        when(aiReviewResultMapper.selectBySubmissionId(10L)).thenReturn(result);
        when(aiAutoReviewService.toResponse(result)).thenReturn(response);

        assertThat(service.getForSubmission(reviewer(30L), 10L)).isEqualTo(response);
    }

    private CurrentUser reviewer(Long userId) {
        return new CurrentUser(userId, "reviewer", "r@test.dev", Set.of(RoleCode.REVIEWER), 1);
    }

    private Submission submission(Long id, Long taskId, Long assignedReviewerId) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setTaskId(taskId);
        submission.setAssignedReviewerId(assignedReviewerId);
        return submission;
    }

    private AiReviewResult result(Long submissionId) {
        AiReviewResult result = new AiReviewResult();
        result.setSubmissionId(submissionId);
        result.setStatus(AiReviewStatus.SUCCESS);
        return result;
    }
}
