package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
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
class AiReviewRecoveryRunnerTest {

    private static final Long SUBMISSION_ID = 100L;

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private AiReviewDispatcher dispatcher;
    @Mock private AiAutoReviewService aiAutoReviewService;
    @Mock private SystemAgentProvider systemAgentProvider;
    @Mock private AuditAppender auditAppender;

    private AiReviewRecoveryRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AiReviewRecoveryRunner(submissionMapper, aiReviewResultMapper, dispatcher,
                aiAutoReviewService, systemAgentProvider, auditAppender);
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));
    }

    @Test
    void successWithDirectApproveRestoresApprovedStatus() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID))
                .thenReturn(result(AiFlowAction.AI_DIRECT_APPROVE));

        runner.run(null);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubmissionStatus.APPROVED);
    }

    @Test
    void successWithDirectRejectRestoresRejectedStatus() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID))
                .thenReturn(result(AiFlowAction.AI_DIRECT_REJECT));

        runner.run(null);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubmissionStatus.REJECTED);
    }

    @Test
    void successWithManualFlowRestoresPendingFinalStatus() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID))
                .thenReturn(result(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW));

        runner.run(null);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubmissionStatus.PENDING_FINAL);
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
