package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAutoReviewServiceTest {

    private static final Long SUBMISSION_ID = 100L;
    private static final Long TASK_ID = 10L;
    private static final Long CONFIG_ID = 20L;
    private static final Long PROVIDER_ID = 30L;
    private static final Long DATASET_ITEM_ID = 40L;
    private static final Long AGENT_RUN_ID = 50L;

    @Mock private SubmissionMapper submissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private DatasetItemMapper datasetItemMapper;
    @Mock private AiReviewConfigMapper aiReviewConfigMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private AiReviewRateLimiter rateLimiter;
    @Mock private LlmGateway llmGateway;
    @Mock private AgentRunService agentRunService;
    @Mock private SystemAgentProvider systemAgentProvider;
    @Mock private AuditAppender auditAppender;
    @Mock private TraceIdProvider traceIdProvider;

    private AiAutoReviewService service;

    @BeforeEach
    void setUp() {
        service = new AiAutoReviewService(submissionMapper, taskMapper, datasetItemMapper, aiReviewConfigMapper,
                aiReviewResultMapper, rateLimiter, llmGateway, agentRunService, systemAgentProvider, auditAppender,
                traceIdProvider);
    }

    @Test
    void successfulReviewStoresAiResultAndMovesSubmissionToPendingFinal() {
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"decision\":\"PASS\"}",
                "{\"decision\":\"PASS\"}",
                Map.of(
                        "decision", "PASS",
                        "averageScore", 92.5,
                        "dimensionScores", Map.of("accuracy", 95),
                        "riskFlags", List.of("none"),
                        "suggestion", "Looks good"
                ),
                88L,
                null,
                null
        ));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));
        when(traceIdProvider.currentTraceId()).thenReturn("trace-ai");

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(response.decision()).isEqualTo("PASS");
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(resultCaptor.getValue().getPromptSnapshot()).contains("Review answer strictly");
        assertThat(resultCaptor.getValue().getRawResponse()).isEqualTo("{\"decision\":\"PASS\"}");
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.PENDING_FINAL);
        verify(agentRunService).complete(eq(AGENT_RUN_ID), any());
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void failedGatewayFallsBackToManualRequiredAndStillMovesSubmissionToPendingFinal() {
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT,
                null,
                null,
                Map.of(),
                3000L,
                "TIMEOUT",
                "Provider timed out"
        ));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.MANUAL_REQUIRED);
        assertThat(response.errorCode()).isEqualTo("TIMEOUT");
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.MANUAL_REQUIRED);
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.FAILED, "Provider timed out");
        verify(submissionMapper).updateById(any(Submission.class));
    }

    @Test
    void existingResultIsReturnedWithoutCallingGatewayAgain() {
        AiReviewResult existing = new AiReviewResult();
        existing.setSubmissionId(SUBMISSION_ID);
        existing.setEffectiveRunId(AGENT_RUN_ID);
        existing.setStatus(AiReviewStatus.SUCCESS);
        existing.setDecision("PASS");
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(existing);

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        verify(llmGateway, never()).review(any());
        verify(aiReviewResultMapper, never()).insert(any(AiReviewResult.class));
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setTaskId(TASK_ID);
        submission.setDatasetItemId(DATASET_ITEM_ID);
        submission.setAnswerJson("{\"answer\":\"ok\"}");
        submission.setStatus(SubmissionStatus.AI_REVIEWING);
        return submission;
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setAiReviewConfigId(CONFIG_ID);
        return task;
    }

    private DatasetItem datasetItem() {
        DatasetItem item = new DatasetItem();
        item.setId(DATASET_ITEM_ID);
        item.setItemJson("{\"text\":\"raw\"}");
        return item;
    }

    private AiReviewConfig config() {
        AiReviewConfig config = new AiReviewConfig();
        config.setId(CONFIG_ID);
        config.setTaskId(TASK_ID);
        config.setProviderId(PROVIDER_ID);
        config.setModelName("qwen-plus");
        config.setPromptTemplate("Review answer strictly");
        config.setScoringDimensionsJson("[\"accuracy\"]");
        config.setPassThreshold(new BigDecimal("90.00"));
        config.setManualReviewThreshold(new BigDecimal("60.00"));
        config.setOutputSchemaJson("{\"type\":\"object\"}");
        config.setPromptVersion("v2");
        return config;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(AGENT_RUN_ID);
        return run;
    }
}
