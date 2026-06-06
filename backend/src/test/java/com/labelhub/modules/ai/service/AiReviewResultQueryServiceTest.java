package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewResultQueryServiceTest {

    private static final Long SUBMISSION_ID = 70L;
    private static final Long TASK_ID = 10L;
    private static final Long CONFIG_ID = 20L;
    private static final String PROMPT_TEMPLATE = "请评估标注结果的准确性和完整性";
    private static final String ANSWER_JSON = "{\"category\":\"电子产品\",\"confidence\":\"high\"}";

    @Mock
    private AiReviewResultMapper aiReviewResultMapper;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private AiReviewConfigMapper aiReviewConfigMapper;
    @Mock
    private AiAutoReviewService aiAutoReviewService;

    @Test
    void returnsPromptTemplateAndLabelerAnswerForSubmissionResult() {
        AiReviewResultQueryService service = new AiReviewResultQueryService(
                aiReviewResultMapper, submissionMapper, taskMapper, aiReviewConfigMapper, aiAutoReviewService);
        CurrentUser owner = new CurrentUser(1L, "owner", "owner@labelhub.dev", Set.of(RoleCode.OWNER), 1);
        Submission submission = submission();
        Task task = task();
        AiReviewConfig config = config();
        AiReviewResult result = result();
        AiReviewResultResponse expected = new AiReviewResultResponse(
                100L, SUBMISSION_ID, 80L, 30L, "qwen-plus", AiReviewStatus.SUCCESS, "PASS",
                "92.50", Map.of("accuracy", 95), "[]", "Looks good", null, null,
                null, false, java.util.List.of(), null, null, null, null,
                PROMPT_TEMPLATE, ANSWER_JSON);

        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(result);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config);
        when(aiAutoReviewService.toResponse(result, PROMPT_TEMPLATE, ANSWER_JSON)).thenReturn(expected);

        AiReviewResultResponse response = service.getForSubmission(owner, SUBMISSION_ID);

        assertThat(response.rawPrompt()).isEqualTo(PROMPT_TEMPLATE);
        assertThat(response.answerJson()).isEqualTo(ANSWER_JSON);
        verify(aiAutoReviewService).toResponse(result, PROMPT_TEMPLATE, ANSWER_JSON);
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setTaskId(TASK_ID);
        submission.setAnswerJson(ANSWER_JSON);
        return submission;
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(1L);
        task.setAiReviewConfigId(CONFIG_ID);
        return task;
    }

    private AiReviewConfig config() {
        AiReviewConfig config = new AiReviewConfig();
        config.setId(CONFIG_ID);
        config.setTaskId(TASK_ID);
        config.setPromptTemplate(PROMPT_TEMPLATE);
        return config;
    }

    private AiReviewResult result() {
        AiReviewResult result = new AiReviewResult();
        result.setId(100L);
        result.setSubmissionId(SUBMISSION_ID);
        result.setStatus(AiReviewStatus.SUCCESS);
        return result;
    }
}
