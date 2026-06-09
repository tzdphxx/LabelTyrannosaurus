package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewFlowStatus;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @Mock private LlmProviderService llmProviderService;
    @Mock private AgentRunService agentRunService;
    @Mock private SystemAgentProvider systemAgentProvider;
    @Mock private AuditAppender auditAppender;
    @Mock private TraceIdProvider traceIdProvider;
    @Mock private AiReviewRetryScheduler retryScheduler;
    @Mock private SupervisorAgent supervisorAgent;
    @Mock private SubmissionEventPublisher eventPublisher;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private DatasetClaimService datasetClaimService;
    @Mock private com.labelhub.infrastructure.redis.RedisLockService redisLockService;
    @Mock private com.labelhub.modules.review.service.ReviewOwnershipResolver reviewOwnershipResolver;
    @Mock private PromptTemplateEngine promptTemplateEngine;

    private AiReviewRetryStrategy retryStrategy;
    private AiAutoReviewService service;

    @BeforeEach
    void setUp() {
        retryStrategy = new AiReviewRetryStrategy();
        org.springframework.transaction.support.TransactionTemplate txTemplate =
                new org.springframework.transaction.support.TransactionTemplate();
        txTemplate.setTransactionManager(new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {}
            @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {}
            @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {}
        });
        service = new AiAutoReviewService(submissionMapper, taskMapper, datasetItemMapper, aiReviewConfigMapper,
                aiReviewResultMapper, rateLimiter, llmGateway, agentRunService, systemAgentProvider, auditAppender,
                traceIdProvider, new com.fasterxml.jackson.databind.ObjectMapper(),
                retryStrategy, retryScheduler, supervisorAgent, txTemplate);
        ReflectionTestUtils.setField(service, "flowDecisionService", new AiFlowDecisionService());
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "reviewRecordMapper", reviewRecordMapper);
        ReflectionTestUtils.setField(service, "datasetClaimService", datasetClaimService);
        ReflectionTestUtils.setField(service, "redisLockService", redisLockService);
        ReflectionTestUtils.setField(service, "reviewOwnershipResolver", reviewOwnershipResolver);
        ReflectionTestUtils.setField(service, "promptTemplateEngine", promptTemplateEngine);
        ReflectionTestUtils.setField(service, "llmProviderService", llmProviderService);
        ReflectionTestUtils.setField(service, "videoKeyFrameService", videoKeyFrameService());
        VoteAggregator voteAggregator = new VoteAggregator();
        ReflectionTestUtils.setField(service, "voteAggregator", voteAggregator);
        ReflectionTestUtils.setField(service, "dimensionAggregator", new DimensionAggregator(voteAggregator));
        ReflectionTestUtils.setField(service, "reviewTraceBuilder", new ReviewTraceBuilder());
        org.mockito.Mockito.lenient()
                .when(promptTemplateEngine.buildReviewPrompt(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("You are LabelHub AI reviewer. Return valid JSON only.");
        org.mockito.Mockito.lenient()
                .when(redisLockService.tryLock(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(agentRunService.create(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(agentRun());
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
        assertThat(resultCaptor.getValue().getPromptMode()).isEqualTo("TEXT_ONLY");
        assertThat(resultCaptor.getValue().getDegraded()).isFalse();
        assertThat(resultCaptor.getValue().getRawResponse()).isEqualTo("{\"decision\":\"PASS\"}");
        assertThat(resultCaptor.getValue().getReviewTrace()).contains("\"strategy\":\"LIGHTWEIGHT\"");
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.PENDING_FINAL);
        verify(agentRunService).complete(eq(AGENT_RUN_ID), any());
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void directApproveCompletesAssignmentGoldenSnapshotAndApprovedCountForSingleOverlap() {
        AiReviewConfig directApproveConfig = config();
        directApproveConfig.setAiFlowPolicy("AI_PASS_ONLY");
        directApproveConfig.setAllowAiDirectApprove(true);
        directApproveConfig.setConfidenceThreshold(new BigDecimal("0.70"));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        Task task = task();
        task.setOverlapCount(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(directApproveConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 95.0, 0.95));
        when(submissionMapper.updateStatusIfCurrentIn(eq(SUBMISSION_ID), eq(SubmissionStatus.APPROVED.name()),
                eq(ReviewFlowStatus.FINAL_APPROVED.name()), eq(true),
                eq(SubmissionStatus.AI_REVIEWING.name()), eq(SubmissionStatus.PENDING_FINAL.name()))).thenReturn(1);
        when(assignmentMapper.selectById(200L)).thenReturn(assignment());
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.flowAction()).isEqualTo("AI_DIRECT_APPROVE");
        verify(submissionMapper).updateStatusIfCurrentIn(SUBMISSION_ID, SubmissionStatus.APPROVED.name(),
                ReviewFlowStatus.FINAL_APPROVED.name(), true,
                SubmissionStatus.AI_REVIEWING.name(), SubmissionStatus.PENDING_FINAL.name());
        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentMapper).updateById(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(AssignmentStatus.APPROVED);
        assertThat(assignmentCaptor.getValue().getApprovedAt()).isNotNull();
        verify(eventPublisher).publishApproved(SUBMISSION_ID, null);
        verify(datasetClaimService).increaseApprovedCount(DATASET_ITEM_ID);
    }

    @Test
    void directApproveDoesNotUpdateAssignmentWhenCasLosesRace() {
        AiReviewConfig directApproveConfig = config();
        directApproveConfig.setAiFlowPolicy("AI_PASS_ONLY");
        directApproveConfig.setAllowAiDirectApprove(true);
        directApproveConfig.setConfidenceThreshold(new BigDecimal("0.70"));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(directApproveConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 95.0, 0.95));
        when(submissionMapper.updateStatusIfCurrentIn(eq(SUBMISSION_ID), eq(SubmissionStatus.APPROVED.name()),
                eq(ReviewFlowStatus.FINAL_APPROVED.name()), eq(true),
                eq(SubmissionStatus.AI_REVIEWING.name()), eq(SubmissionStatus.PENDING_FINAL.name()))).thenReturn(0);
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.flowAction()).isEqualTo("AI_DIRECT_APPROVE");
        verify(assignmentMapper, never()).selectById(anyLong());
        verify(assignmentMapper, never()).updateById(any(Assignment.class));
        verify(datasetClaimService, never()).increaseApprovedCount(anyLong());
    }

    @Test
    void directApproveForOverlappedTaskFallsBackToManualReview() {
        AiReviewConfig directApproveConfig = config();
        directApproveConfig.setAiFlowPolicy("AI_PASS_ONLY");
        directApproveConfig.setAllowAiDirectApprove(true);
        directApproveConfig.setConfidenceThreshold(new BigDecimal("0.70"));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        Task task = task();
        task.setOverlapCount(2);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(directApproveConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 95.0, 0.95));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.flowAction()).isEqualTo("AI_ASSIGN_MANUAL_REVIEW");
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).updateById(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.PENDING_FINAL);
        verify(eventPublisher, never()).publishApproved(anyLong(), any());
        verify(datasetClaimService, never()).increaseApprovedCount(anyLong());
    }

    @Test
    void directRejectReturnsAssignmentAndRecordsVisibleReason() {
        AiReviewConfig directRejectConfig = config();
        directRejectConfig.setAiFlowPolicy("AI_REJECT_ONLY");
        directRejectConfig.setAllowAiDirectReject(true);
        directRejectConfig.setRejectThreshold(new BigDecimal("50.00"));
        directRejectConfig.setConfidenceThreshold(new BigDecimal("0.70"));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(directRejectConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("REJECT", 20.0, 0.93));
        when(submissionMapper.updateStatusIfCurrentIn(eq(SUBMISSION_ID), eq(SubmissionStatus.REJECTED.name()),
                eq(ReviewFlowStatus.REJECTED.name()), eq(null),
                eq(SubmissionStatus.AI_REVIEWING.name()), eq(SubmissionStatus.PENDING_FINAL.name()))).thenReturn(1);
        when(assignmentMapper.selectById(200L)).thenReturn(assignment());
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.flowAction()).isEqualTo("AI_DIRECT_REJECT");
        verify(submissionMapper).updateStatusIfCurrentIn(SUBMISSION_ID, SubmissionStatus.REJECTED.name(),
                ReviewFlowStatus.REJECTED.name(), null,
                SubmissionStatus.AI_REVIEWING.name(), SubmissionStatus.PENDING_FINAL.name());
        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentMapper).updateById(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(AssignmentStatus.RETURNED);
        assertThat(assignmentCaptor.getValue().getReturnedAt()).isNotNull();
        ArgumentCaptor<ReviewRecord> recordCaptor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo(ReviewAction.AI_DIRECT_REJECT);
        assertThat(recordCaptor.getValue().getReason()).isEqualTo("Looks good");
    }

    @Test
    void directRejectDoesNotUpdateAssignmentWhenCasLosesRace() {
        AiReviewConfig directRejectConfig = config();
        directRejectConfig.setAiFlowPolicy("AI_REJECT_ONLY");
        directRejectConfig.setAllowAiDirectReject(true);
        directRejectConfig.setRejectThreshold(new BigDecimal("50.00"));
        directRejectConfig.setConfidenceThreshold(new BigDecimal("0.70"));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(directRejectConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("REJECT", 20.0, 0.93));
        when(submissionMapper.updateStatusIfCurrentIn(eq(SUBMISSION_ID), eq(SubmissionStatus.REJECTED.name()),
                eq(ReviewFlowStatus.REJECTED.name()), eq(null),
                eq(SubmissionStatus.AI_REVIEWING.name()), eq(SubmissionStatus.PENDING_FINAL.name()))).thenReturn(0);
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.flowAction()).isEqualTo("AI_DIRECT_REJECT");
        verify(assignmentMapper, never()).selectById(anyLong());
        verify(assignmentMapper, never()).updateById(any(Assignment.class));
        verify(eventPublisher, never()).publishRejected(anyLong(), any(), anyString());
        verify(reviewRecordMapper, never()).insert(any(ReviewRecord.class));
    }

    @Test
    void failedGatewaySchedulesRetryWhenRetriesRemaining() {
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

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(response.errorCode()).isEqualTo("TIMEOUT");
        verify(retryScheduler).scheduleRetry(eq(SUBMISSION_ID), any(Duration.class));
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.FAILED, "Provider timed out");
        verify(submissionMapper).updateById(any(Submission.class));
        verify(auditAppender, never()).append(any(AuditCommand.class));
    }

    @Test
    void failedGatewayFallsBackToManualRequiredWhenMaxRetryIsZero() {
        AiReviewConfig zeroRetryConfig = config();
        zeroRetryConfig.setMaxRetry(0);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(zeroRetryConfig);
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
        verify(retryScheduler, never()).scheduleRetry(anyLong(), any(Duration.class));
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.FAILED, "Provider timed out");
        verify(submissionMapper).updateById(any(Submission.class));
    }

    @Test
    void invalidAiOutputGoesDirectlyToManualRequiredWithoutRetry() {
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
                "not json",
                "not json",
                Map.of(),
                50L,
                null,
                null
        ));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.MANUAL_REQUIRED);
        assertThat(response.errorCode()).isEqualTo("INVALID_AI_REVIEW_OUTPUT");
        verify(retryScheduler, never()).scheduleRetry(anyLong(), any(Duration.class));
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.MANUAL_REQUIRED, "AI 审核结论不能为空");
    }

    @Test
    void thrownLlmKeyDecryptFailureStoresFailedResultInsteadOfLeavingSubmissionWithoutResult() {
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class)))
                .thenThrow(new BusinessException(500302, "LLM key decrypt failed"));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(response.errorCode()).isEqualTo("LLM_KEY_DECRYPT_FAILED");
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getSubmissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(resultCaptor.getValue().getEffectiveRunId()).isEqualTo(AGENT_RUN_ID);
        assertThat(resultCaptor.getValue().getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(resultCaptor.getValue().getModelName()).isEqualTo("qwen-plus");
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(resultCaptor.getValue().getNextRetryAt()).isNull();
        assertThat(resultCaptor.getValue().getErrorCode()).isEqualTo("LLM_KEY_DECRYPT_FAILED");
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.FAILED, "LLM key decrypt failed");
        verify(retryScheduler, never()).scheduleRetry(anyLong(), any(Duration.class));
    }

    @Test
    void videoDirectFailureFallsBackToCosKeyFramesForReview() {
        DatasetItem item = datasetItem();
        item.setItemJson("""
                {
                  "media_type": "video",
                  "media_url": "https://bucket-123.cos.ap-guangzhou.myqcloud.com/videos/oceans.mp4",
                  "media_processing_status": "READY"
                }
                """);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(item);
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(new LlmProvider()));
        when(llmProviderService.capability(any(LlmProvider.class)))
                .thenReturn(new ProviderCapability(true, true, 5, null));
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class)))
                .thenReturn(new LlmGatewayResponse(LlmGatewayStatus.PROVIDER_ERROR, null, null, null,
                        500L, "UNSUPPORTED_VIDEO", "video_url is not supported"))
                .thenReturn(successGateway("PASS", 91.0, 0.88));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.SUCCESS);
        ArgumentCaptor<LlmGatewayRequest> requestCaptor = ArgumentCaptor.forClass(LlmGatewayRequest.class);
        verify(llmGateway, org.mockito.Mockito.times(2)).review(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).messages())
                .anySatisfy(message -> assertThat(message.contentParts())
                        .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.VideoUrlPart.class)));
        assertThat(requestCaptor.getAllValues().get(1).messages())
                .anySatisfy(message -> assertThat(message.contentParts())
                        .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.ImageUrlPart.class)));
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getPromptMode()).isEqualTo("VIDEO_KEYFRAMES");
    }

    @Test
    void parallelVoteLlmKeyDecryptFailureStoresFailedResult() {
        AiReviewConfig parallelConfig = config();
        parallelConfig.setReviewStrategy("PARALLEL_VOTE");
        parallelConfig.setVoteModelsJson("""
                [{"providerId":30,"modelName":"qwen-plus"},{"providerId":30,"modelName":"qwen-plus"}]
                """);
        parallelConfig.setVoteMinAgreement(2);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(parallelConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class)))
                .thenThrow(new BusinessException(500302, "LLM key decrypt failed"));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(response.errorCode()).isEqualTo("LLM_KEY_DECRYPT_FAILED");
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(resultCaptor.getValue().getNextRetryAt()).isNull();
        verify(retryScheduler, never()).scheduleRetry(anyLong(), any(Duration.class));
    }

    @Test
    void parallelVoteAllAsyncBranchesFailDoesNotCreateSyntheticReject() {
        AiReviewConfig parallelConfig = config();
        parallelConfig.setReviewStrategy("PARALLEL_VOTE");
        parallelConfig.setVoteModelsJson("""
                [{"providerId":30,"modelName":"qwen-plus"},{"providerId":30,"modelName":"qwen-plus"}]
                """);
        parallelConfig.setVoteMinAgreement(2);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(parallelConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT,
                null,
                null,
                null,
                null,
                "TIMEOUT",
                "LLM request timed out"
        ));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(response.decision()).isNull();
        assertThat(response.errorCode()).isEqualTo("TIMEOUT");
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(resultCaptor.getValue().getDecision()).isNull();
    }

    @Test
    void deepDimensionLlmKeyDecryptFailureStoresFailedResult() {
        AiReviewConfig deepConfig = config();
        deepConfig.setReviewStrategy("DEEP_DIMENSION");
        deepConfig.setScoringDimensionsJson("[\"accuracy\",\"completeness\"]");
        deepConfig.setDimensionReviewersJson("""
                {"accuracy":[{"providerId":30,"modelName":"qwen-plus"}],
                 "completeness":[{"providerId":30,"modelName":"qwen-plus"}]}
                """);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(deepConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class)))
                .thenThrow(new BusinessException(500302, "LLM key decrypt failed"));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(response.errorCode()).isEqualTo("LLM_KEY_DECRYPT_FAILED");
        assertThat(response.decision()).isNull();
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(resultCaptor.getValue().getNextRetryAt()).isNull();
        verify(retryScheduler, never()).scheduleRetry(anyLong(), any(Duration.class));
    }

    @Test
    void deepDimensionSuccessStoresImagePromptMetadata() {
        AiReviewConfig deepConfig = config();
        deepConfig.setReviewStrategy("DEEP_DIMENSION");
        deepConfig.setScoringDimensionsJson("[\"accuracy\"]");
        ReflectionTestUtils.setField(service, "mediaPromptContextBuilder", (MediaPromptContextBuilder) input ->
                new MediaPromptResult(
                        List.of(LlmMessage.userParts(List.of(
                                new LlmMessage.TextPart("Review this image answer"),
                                new LlmMessage.ImageUrlPart("https://www.w3schools.com/w3css/img_lights.jpg", "auto")
                        ))),
                        PromptMode.IMAGE_SINGLE,
                        false,
                        List.of(),
                        "Review answer strictly\n[image]",
                        Map.of("usedMedia", true, "imageCount", 1)
                ));
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(deepConfig);
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 95.0, 0.95));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.SUCCESS);
        ArgumentCaptor<AiReviewResult> resultCaptor = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(aiReviewResultMapper).insert(resultCaptor.capture());
        AiReviewResult result = resultCaptor.getValue();
        assertThat(result.getStatus()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(result.getPromptMode()).isEqualTo("IMAGE_SINGLE");
        assertThat(result.getDegraded()).isFalse();
        assertThat(result.getLimitations()).isEqualTo("[]");
    }

    @Test
    void rateLimitedSchedulesRetryWithLongerDelay() {
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(false);
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun());

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.RATE_LIMITED);
        ArgumentCaptor<Duration> delayCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(retryScheduler).scheduleRetry(eq(SUBMISSION_ID), delayCaptor.capture());
        assertThat(delayCaptor.getValue().toMillis()).isGreaterThanOrEqualTo(8000L);
    }

    @Test
    void retryReviewSucceedsAfterPreviousFailure() {
        AiReviewResult failedResult = new AiReviewResult();
        failedResult.setSubmissionId(SUBMISSION_ID);
        failedResult.setStatus(AiReviewStatus.FAILED);
        failedResult.setRetryCount(1);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(failedResult);
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
                Map.of("decision", "PASS", "averageScore", 85.0),
                100L,
                null,
                null
        ));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));
        when(traceIdProvider.currentTraceId()).thenReturn("trace-retry");

        service.retryReview(SUBMISSION_ID);

        verify(agentRunService).complete(eq(AGENT_RUN_ID), any());
        verify(aiReviewResultMapper).updateForSuccess(eq(SUBMISSION_ID), eq("SUCCESS"),
                eq(AGENT_RUN_ID), eq("PASS"), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void existingResultIsReturnedWithoutCallingGatewayAgain() {
        AiReviewResult existing = new AiReviewResult();
        existing.setSubmissionId(SUBMISSION_ID);
        existing.setEffectiveRunId(AGENT_RUN_ID);
        existing.setStatus(AiReviewStatus.SUCCESS);
        existing.setDecision("PASS");
        existing.setReviewTrace("""
                {"strategy":"PARALLEL_VOTE","strategyLabel":"Parallel model vote","summary":"3 branches","steps":[],"metrics":{"voteCount":3}}
                """);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(existing);

        AiReviewResultResponse response = service.reviewSubmission(SUBMISSION_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        assertThat(response.reviewTrace()).isNotNull();
        assertThat(response.reviewTrace().strategy()).isEqualTo("PARALLEL_VOTE");
        assertThat(response.reviewTrace().metrics()).containsEntry("voteCount", 3);
        verify(llmGateway, never()).review(any());
        verify(aiReviewResultMapper, never()).insert(any(AiReviewResult.class));
    }

    @Test
    void queuedReviewReusesPendingAgentRun() {
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(agentRunService.findPending(AGENT_RUN_ID)).thenReturn(Optional.of(agentRun()));
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 91.0, 0.88));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.executeQueuedReview(SUBMISSION_ID, AGENT_RUN_ID);

        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        verify(agentRunService).findPending(AGENT_RUN_ID);
        verify(agentRunService).start(AGENT_RUN_ID);
        verify(agentRunService, never()).create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID),
                eq("qwen-plus"), eq("v2"), any());
    }

    @Test
    void queuedReviewCreatesNewAgentRunWhenQueuedRunAlreadyFailed() {
        Long staleRunId = 51L;
        Long newRunId = 52L;
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(null);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(agentRunService.findPending(staleRunId)).thenReturn(Optional.empty());
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(agentRun(newRunId));
        when(agentRunService.create(eq("AI_REVIEW"), eq(SUBMISSION_ID), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any(), org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(agentRun(newRunId));
        when(rateLimiter.acquire(TASK_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(successGateway("PASS", 91.0, 0.88));
        when(systemAgentProvider.get()).thenReturn(new SystemActorContext(900L));

        AiReviewResultResponse response = service.executeQueuedReview(SUBMISSION_ID, staleRunId);

        assertThat(response.agentRunId()).isEqualTo(newRunId);
        verify(agentRunService).findPending(staleRunId);
        verify(agentRunService, never()).start(staleRunId);
        verify(agentRunService).start(newRunId);
    }

    @Test
    void queuedReviewReturnsTerminalResultWithoutStartingAgentRun() {
        AiReviewResult existing = new AiReviewResult();
        existing.setSubmissionId(SUBMISSION_ID);
        existing.setEffectiveRunId(AGENT_RUN_ID);
        existing.setStatus(AiReviewStatus.MANUAL_REQUIRED);
        when(aiReviewResultMapper.selectBySubmissionId(SUBMISSION_ID)).thenReturn(existing);

        AiReviewResultResponse response = service.executeQueuedReview(SUBMISSION_ID, AGENT_RUN_ID);

        assertThat(response.status()).isEqualTo(AiReviewStatus.MANUAL_REQUIRED);
        verify(agentRunService, never()).findPending(anyLong());
        verify(agentRunService, never()).start(anyLong());
        verify(llmGateway, never()).review(any());
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setAssignmentId(200L);
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
        task.setOverlapCount(1);
        return task;
    }

    private Assignment assignment() {
        Assignment assignment = new Assignment();
        assignment.setId(200L);
        assignment.setStatus(AssignmentStatus.SUBMITTED);
        return assignment;
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
        return agentRun(AGENT_RUN_ID);
    }

    private AgentRun agentRun(Long id) {
        AgentRun run = new AgentRun();
        run.setId(id);
        return run;
    }

    private LlmGatewayResponse successGateway(String decision, double averageScore, double confidence) {
        return new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"decision\":\"" + decision + "\"}",
                "{\"decision\":\"" + decision + "\"}",
                Map.of(
                        "decision", decision,
                        "averageScore", averageScore,
                        "confidence", confidence,
                        "dimensionScores", Map.of("accuracy", averageScore),
                        "riskFlags", List.of(),
                        "suggestion", "Looks good"
                ),
                88L,
                null,
                null
        );
    }

    private VideoKeyFrameService videoKeyFrameService() {
        VideoKeyFrameService service = new VideoKeyFrameService();
        ReflectionTestUtils.setField(service, "maxFrames", 5);
        ReflectionTestUtils.setField(service, "intervalSeconds", 5);
        return service;
    }
}
