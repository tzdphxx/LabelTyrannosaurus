package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewRecoveryRunnerTest {

    private static final Long SUBMISSION_ID = 100L;

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private AiReviewDispatcher dispatcher;
    @Mock private AiAutoReviewService aiAutoReviewService;
    @Mock private SystemAgentProvider systemAgentProvider;
    @Mock private AuditAppender auditAppender;
    @Mock private com.labelhub.modules.review.service.ReviewOwnershipResolver reviewOwnershipResolver;
    @Mock private AiReviewSchemaReadiness schemaReadiness;
    @Mock private RedisLockService redisLockService;

    private AiReviewRecoveryRunner runner;
    private final ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    private final ArgumentCaptor<AiReviewResult> aiReviewResultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);

    @BeforeEach
    void setUp() {
        runner = new AiReviewRecoveryRunner(submissionMapper, aiReviewResultMapper, dispatcher,
                aiAutoReviewService, systemAgentProvider, auditAppender, reviewOwnershipResolver,
                schemaReadiness, redisLockService);
        when(schemaReadiness.isReady()).thenReturn(true);
        lenient().when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(3);
            action.run();
            return null;
        }).when(redisLockService).withLock(any(), anyLong(), anyLong(), any(Runnable.class));
    }

    @Test
    void skipsRecoveryWhenSchemaIsNotReady() {
        when(schemaReadiness.isReady()).thenReturn(false);

        runner.run(null);

        verify(submissionMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(aiReviewResultMapper, never()).selectBySubmissionId(any());
    }

    @Test
    void successWithDirectApproveDelegatesToAutoReviewService() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        AiReviewResult result = result(AiFlowAction.AI_DIRECT_APPROVE);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(result);

        runner.run(null);

        verify(aiAutoReviewService).applyRecoveredFlowAction(submissionCaptor.capture(),
                aiReviewResultCaptor.capture());
        assertThat(submissionCaptor.getValue().getId()).isEqualTo(SUBMISSION_ID);
        assertThat(aiReviewResultCaptor.getValue().getFlowAction()).isEqualTo(AiFlowAction.AI_DIRECT_APPROVE.name());
    }

    @Test
    void successWithDirectRejectDelegatesToAutoReviewService() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        AiReviewResult result = result(AiFlowAction.AI_DIRECT_REJECT);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(result);

        runner.run(null);

        verify(aiAutoReviewService).applyRecoveredFlowAction(submissionCaptor.capture(),
                aiReviewResultCaptor.capture());
        assertThat(submissionCaptor.getValue().getId()).isEqualTo(SUBMISSION_ID);
        assertThat(aiReviewResultCaptor.getValue().getFlowAction()).isEqualTo(AiFlowAction.AI_DIRECT_REJECT.name());
    }

    @Test
    void successWithManualFlowRestoresPendingFinalStatus() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        AiReviewResult result = result(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(result);

        runner.run(null);

        verify(aiAutoReviewService).applyRecoveredFlowAction(submissionCaptor.capture(),
                aiReviewResultCaptor.capture());
        assertThat(submissionCaptor.getValue().getId()).isEqualTo(SUBMISSION_ID);
        assertThat(aiReviewResultCaptor.getValue().getFlowAction()).isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW.name());
    }

    @Test
    void periodicRecoveryRequeuesAiReviewingSubmissionWithoutResult() {
        Submission submission = submission();
        submission.setSubmittedAt(LocalDateTime.now().minusMinutes(2));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);

        runner.recoverMissingResultsPeriodically();

        verify(dispatcher).enqueue(SUBMISSION_ID);
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setStatus(SubmissionStatus.AI_REVIEWING);
        return submission;
    }

    private AiReviewResult result(AiFlowAction flowAction) {
        AiReviewResult result = new AiReviewResult();
        result.setSubmissionId(SUBMISSION_ID);
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setFlowAction(flowAction.name());
        return result;
    }
}
