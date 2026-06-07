package com.labelhub.modules.ai.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewRetrySchedulerTest {

    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private LlmTaskQueueService queueService;
    @Mock private TraceIdProvider traceIdProvider;
    @Mock private AiReviewSchemaReadiness schemaReadiness;

    private AiReviewRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AiReviewRetryScheduler(aiReviewResultMapper, submissionMapper, queueService,
                traceIdProvider, schemaReadiness);
        when(schemaReadiness.isReady()).thenReturn(true);
    }

    @Test
    void skipsRetryScanWhenSchemaIsNotReady() {
        when(schemaReadiness.isReady()).thenReturn(false);

        scheduler.enqueueDueRetries();

        verify(aiReviewResultMapper, never()).selectPendingRetries();
        verify(queueService, never()).enqueue(any(LlmTaskQueueMessage.class));
    }

    @Test
    void enqueueDueRetriesKeepsExistingBehaviorWhenSchemaIsReady() {
        AiReviewResult result = new AiReviewResult();
        result.setSubmissionId(100L);
        result.setEffectiveRunId(200L);
        result.setRetryCount(1);
        Submission submission = new Submission();
        submission.setId(100L);
        submission.setTaskId(300L);
        submission.setAssignmentId(400L);

        when(aiReviewResultMapper.selectPendingRetries()).thenReturn(List.of(result));
        when(submissionMapper.selectById(100L)).thenReturn(submission);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");

        scheduler.enqueueDueRetries();

        verify(queueService).enqueue(any(LlmTaskQueueMessage.class));
    }
}
