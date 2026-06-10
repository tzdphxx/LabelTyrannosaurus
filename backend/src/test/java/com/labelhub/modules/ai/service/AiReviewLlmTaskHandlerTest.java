package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewLlmTaskHandlerTest {

    @Mock private AiAutoReviewService aiAutoReviewService;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private SubmissionMapper submissionMapper;

    @Test
    void completedWhenResultIsTerminal() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L)).thenReturn(result(AiReviewStatus.SUCCESS));

        assertThat(handler.isCompleted(message())).isTrue();
    }

    @Test
    void completedWhenFailedOrRateLimitedResultHasNoNextRetry() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L))
                .thenReturn(result(AiReviewStatus.FAILED))
                .thenReturn(result(AiReviewStatus.RATE_LIMITED));

        assertThat(handler.isCompleted(message())).isTrue();
        assertThat(handler.isCompleted(message())).isTrue();
    }

    @Test
    void notCompletedWhenFailedOrRateLimitedResultCanBeRetried() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L))
                .thenReturn(retryableResult(AiReviewStatus.FAILED))
                .thenReturn(retryableResult(AiReviewStatus.RATE_LIMITED));

        assertThat(handler.isCompleted(message())).isFalse();
        assertThat(handler.isCompleted(message())).isFalse();
    }

    @Test
    void handleExecutesQueuedReviewWhenResultIsMissing() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L)).thenReturn(null);

        handler.handle(message());

        verify(aiAutoReviewService).executeQueuedReview(100L, 300L);
        verify(aiAutoReviewService, never()).retryReview(100L);
    }

    @Test
    void handleRetriesWhenResultIsRetryableFailure() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L)).thenReturn(result(AiReviewStatus.FAILED));

        handler.handle(message());

        verify(aiAutoReviewService).retryReview(100L);
        verify(aiAutoReviewService, never()).executeQueuedReview(100L, 300L);
    }

    @Test
    void completedWhenResultAndSubmissionAreMissing() {
        AiReviewLlmTaskHandler handler = new AiReviewLlmTaskHandler(
                aiAutoReviewService, aiReviewResultMapper, submissionMapper);
        when(aiReviewResultMapper.selectBySubmissionId(100L)).thenReturn(null);
        when(submissionMapper.selectById(100L)).thenReturn(null);

        assertThat(handler.isCompleted(message())).isTrue();
    }

    private AiReviewResult result(AiReviewStatus status) {
        AiReviewResult result = new AiReviewResult();
        result.setSubmissionId(100L);
        result.setStatus(status);
        return result;
    }

    private AiReviewResult retryableResult(AiReviewStatus status) {
        AiReviewResult result = result(status);
        result.setNextRetryAt(LocalDateTime.now().plusMinutes(1));
        return result;
    }

    private LlmTaskQueueMessage message() {
        return new LlmTaskQueueMessage(
                LlmTaskType.AI_REVIEW,
                100L,
                10L,
                20L,
                100L,
                null,
                null,
                300L,
                "trace-ai",
                0,
                Instant.parse("2026-06-03T00:00:00Z")
        );
    }
}
