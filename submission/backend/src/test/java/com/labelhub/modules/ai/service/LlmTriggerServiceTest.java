package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskStatus;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.LlmTriggerRunMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LlmTriggerServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long LABELER_ID = 2L;
    private static final Long TASK_ID = 10L;
    private static final Long DATASET_ITEM_ID = 30L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long PROVIDER_ID = 50L;
    private static final Long AGENT_RUN_ID = 60L;
    private static final Long TRIGGER_RUN_ID = 70L;
    private static final Long AI_REVIEW_CONFIG_ID = 80L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DatasetItemMapper datasetItemMapper;

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private LlmProviderService llmProviderService;

    @Mock
    private LlmTriggerRateLimiter rateLimiter;

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AgentRunService agentRunService;

    @Mock
    private AuditAppender auditAppender;

    @Mock
    private TraceIdProvider traceIdProvider;

    @Mock
    private LlmTaskQueueService llmTaskQueueService;

    @Mock
    private LlmTriggerAsyncExecutor llmTriggerAsyncExecutor;

    @Mock
    private LlmTriggerRunMapper llmTriggerRunMapper;

    @Mock
    private TemplateVersionMapper templateVersionMapper;

    @Mock
    private AiReviewConfigMapper aiReviewConfigMapper;

    @Mock
    private PromptTemplateEngine promptTemplateEngine;

    private LlmTriggerService service;

    @BeforeEach
    void setUp() {
        service = new LlmTriggerService(taskMapper, datasetItemMapper, assignmentMapper,
                llmProviderService, rateLimiter, llmGateway, agentRunService, auditAppender, traceIdProvider,
                llmTaskQueueService, llmTriggerAsyncExecutor,
                llmTriggerRunMapper, templateVersionMapper, aiReviewConfigMapper,
                new com.fasterxml.jackson.databind.ObjectMapper());
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
        org.springframework.test.util.ReflectionTestUtils.setField(service, "promptTemplateEngine",
                promptTemplateEngine);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "videoKeyFrameService",
                videoKeyFrameService());
        org.mockito.Mockito.lenient()
                .when(promptTemplateEngine.buildLlmTriggerPrompt(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("You are a LabelHub whole-template LlmTrigger assistant.");
    }

    @Test
    void labelerTriggersFromAssignmentAndEnqueuesRunUsingTaskConfigAndTemplateContext() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), isNull(), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v1"), any(), eq(ASSIGNMENT_ID), eq("trace-1"))).thenReturn(agentRun());
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        doAnswer(invocation -> {
            LlmTriggerRun run = invocation.getArgument(0);
            run.setId(TRIGGER_RUN_ID);
            return 1;
        }).when(llmTriggerRunMapper).insert(any(LlmTriggerRun.class));

        LlmTriggerRunResponse response = service.runForAssignment(labeler(), ASSIGNMENT_ID, componentRequest());

        assertThat(response.triggerRunId()).isEqualTo(TRIGGER_RUN_ID);
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        assertThat(response.status()).isEqualTo(LlmTaskStatus.RUNNING.name());
        assertThat(response.targetFields()).containsExactly("summary");
        assertThat(response.componentId()).isEqualTo(20L);

        ArgumentCaptor<LlmTriggerRun> runCaptor = ArgumentCaptor.forClass(LlmTriggerRun.class);
        verify(llmTriggerRunMapper).insert(runCaptor.capture());
        LlmTriggerRun insertedRun = runCaptor.getValue();
        assertThat(insertedRun.getComponentId()).isEqualTo("20");
        assertThat(insertedRun.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(insertedRun.getModelName()).isEqualTo("qwen-plus");
        assertThat(insertedRun.getInputSnapshotJson())
                .contains("\"scoringDimensions\":[\"accuracy\",\"clarity\"]")
                .contains("\"componentId\":20")
                .contains("\"targetFields\":[\"summary\"]")
                .contains("\"userInstruction\":\"Make it concise\"");

        verify(agentRunService).start(AGENT_RUN_ID);
        verify(llmTriggerAsyncExecutor).submit(TRIGGER_RUN_ID, "trace-1");
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void labelerRequiresOwnedAssignment() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.runForAssignment(labeler(), ASSIGNMENT_ID, componentRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404504));
    }

    @Test
    void ownerTestRequiresTaskOwnership() {
        Task task = task();
        task.setOwnerId(99L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> service.testFromTask(owner(), TASK_ID, requestWithItem()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void ownerTestEnqueuesLlmTrigger() {
        Task task = task();
        task.setPublishedTemplateVersionId(20L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), isNull(), eq(PROVIDER_ID), eq("qwen-plus"),
                any(), any(), isNull(), eq("trace-1"))).thenReturn(agentRun());
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        doAnswer(invocation -> {
            LlmTriggerRun run = invocation.getArgument(0);
            run.setId(TRIGGER_RUN_ID);
            return 1;
        }).when(llmTriggerRunMapper).insert(any(LlmTriggerRun.class));

        LlmTriggerRunResponse response = service.testFromTask(owner(), TASK_ID, requestWithItem());

        assertThat(response.triggerRunId()).isEqualTo(TRIGGER_RUN_ID);
        verify(llmTriggerAsyncExecutor).submit(TRIGGER_RUN_ID, "trace-1");
    }

    @Test
    void rejectsDisabledProvider() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runForAssignment(labeler(), ASSIGNMENT_ID, componentRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400503));
    }

    @Test
    void requiresTaskAiReviewConfigForAssignmentTrigger() {
        Task task = task();
        task.setAiReviewConfigId(null);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());

        assertThatThrownBy(() -> service.runForAssignment(labeler(), ASSIGNMENT_ID, componentRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> {
                            assertThat(ex.getCode()).isEqualTo(400501);
                            assertThat(ex.getMessage()).contains("AI review config");
                        });
    }

    @Test
    void acceptsComponentIdAsEchoOnly() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());

        LlmTriggerRunRequest request = new LlmTriggerRunRequest(
                null, null, null, null, null,
                999L, Map.of("summary", "draft"), null);

        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), isNull(), eq(PROVIDER_ID), eq("qwen-plus"),
                any(), any(), eq(ASSIGNMENT_ID), any())).thenReturn(agentRun());
        doAnswer(invocation -> {
            LlmTriggerRun run = invocation.getArgument(0);
            run.setId(TRIGGER_RUN_ID);
            return 1;
        }).when(llmTriggerRunMapper).insert(any(LlmTriggerRun.class));

        LlmTriggerRunResponse response = service.runForAssignment(labeler(), ASSIGNMENT_ID, request);

        assertThat(response.componentId()).isEqualTo(999L);
    }

    @Test
    void labelerCannotUseOwnerTestEndpoint() {
        assertThatThrownBy(() -> service.testFromTask(labeler(), TASK_ID, requestWithItem()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void workerMarksRunFailedWhenGatewayFails() {
        LlmTriggerRun run = triggerRun();
        when(llmTriggerRunMapper.selectById(TRIGGER_RUN_ID)).thenReturn(run);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT, null, null, Map.of(), 3000L, "TIMEOUT", "Provider timed out"));

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        assertThat(run.getStatus()).isEqualTo(LlmTaskStatus.FAILED.name());
        assertThat(run.getErrorMessage()).isEqualTo("Provider timed out");
    }

    @Test
    void workerNormalizesStructuredPatchAndDropsNonSchemaFields() {
        LlmTriggerRun run = triggerRun();
        when(llmTriggerRunMapper.selectById(TRIGGER_RUN_ID)).thenReturn(run);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"ok\":true}",
                "summary text",
                Map.of(
                        "componentId", "summary",
                        "targetFields", List.of("summary"),
                        "patch", Map.of("summary", "AI summary", "other", "should be dropped"),
                        "displayText", "AI summary",
                        "confidence", 0.91,
                        "warnings", List.of()
                ),
                1200L,
                null,
                null));

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        assertThat(run.getStatus()).isEqualTo(LlmTaskStatus.SUCCESS.name());
        assertThat(run.getResultJson())
                .contains("\"componentId\":20")
                .contains("\"summary\":\"AI summary\"")
                .doesNotContain("should be dropped")
                .contains("Dropped non-schema patch field: other");
    }

    @Test
    void workerSendsImageItemAsMultimodalContentParts() {
        LlmTriggerRun run = triggerRun();
        run.setInputSnapshotJson("""
                {
                    "itemSnapshot": {
                      "media_type": "image",
                      "media_url": "https://www.w3schools.com/w3css/img_lights.jpg",
                      "media_processing_status": "READY"
                    },
                  "currentAnswerJson": {"summary": "draft"}
                }
                """);
        when(llmTriggerRunMapper.selectById(TRIGGER_RUN_ID)).thenReturn(run);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(llmProviderService.capability(any(LlmProvider.class)))
                .thenReturn(new ProviderCapability(true, true, 5, null));
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"ok\":true}",
                "summary text",
                Map.of("patch", Map.of("summary", "AI summary")),
                1200L,
                null,
                null));

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        ArgumentCaptor<LlmGatewayRequest> requestCaptor = ArgumentCaptor.forClass(LlmGatewayRequest.class);
        verify(llmGateway).review(requestCaptor.capture());
        assertThat(requestCaptor.getValue().messages())
                .anySatisfy(message -> assertThat(message.contentParts())
                        .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.ImageUrlPart.class)));
    }

    @Test
    void workerFallsBackFromVideoDirectToCosKeyFrames() {
        LlmTriggerRun run = triggerRun();
        run.setInputSnapshotJson("""
                {
                    "itemSnapshot": {
                      "media_type": "video",
                      "media_url": "https://bucket-123.cos.ap-guangzhou.myqcloud.com/videos/oceans.mp4",
                      "media_processing_status": "READY"
                    },
                  "currentAnswerJson": {"summary": "draft"}
                }
                """);
        when(llmTriggerRunMapper.selectById(TRIGGER_RUN_ID)).thenReturn(run);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(20L)).thenReturn(templateVersion());
        when(aiReviewConfigMapper.selectById(AI_REVIEW_CONFIG_ID)).thenReturn(aiReviewConfig());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(llmProviderService.capability(any(LlmProvider.class)))
                .thenReturn(new ProviderCapability(true, true, 5, null));
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class)))
                .thenReturn(new LlmGatewayResponse(LlmGatewayStatus.PROVIDER_ERROR, null, null, null,
                        500L, "UNSUPPORTED_VIDEO", "video_url is not supported"))
                .thenReturn(new LlmGatewayResponse(
                        LlmGatewayStatus.SUCCESS,
                        "{\"ok\":true}",
                        "summary text",
                        Map.of("patch", Map.of("summary", "AI summary")),
                        1200L,
                        null,
                        null));

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        assertThat(run.getStatus()).isEqualTo(LlmTaskStatus.SUCCESS.name());
        ArgumentCaptor<LlmGatewayRequest> requestCaptor = ArgumentCaptor.forClass(LlmGatewayRequest.class);
        verify(llmGateway, org.mockito.Mockito.times(2)).review(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).messages())
                .anySatisfy(message -> assertThat(message.contentParts())
                        .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.VideoUrlPart.class)));
        assertThat(requestCaptor.getAllValues().get(1).messages())
                .anySatisfy(message -> assertThat(message.contentParts())
                        .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.ImageUrlPart.class)));
    }

    @Test
    void rateLimitedWorkerRunSkipsGateway() {
        LlmTriggerRun run = triggerRun();
        when(llmTriggerRunMapper.selectById(TRIGGER_RUN_ID)).thenReturn(run);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(false);

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        assertThat(run.getStatus()).isEqualTo(LlmTaskStatus.RATE_LIMITED.name());
        assertThat(run.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    private CurrentUser owner() {
        return new CurrentUser(OWNER_ID, "owner", "test@labelhub.dev", Set.of(RoleCode.OWNER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(LABELER_ID, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1);
    }

    private LlmTriggerRunRequest request() {
        return new LlmTriggerRunRequest(
                PROVIDER_ID, "qwen-plus",
                "Suggest a concise summary.",
                List.of("summary"),
                null, null, Map.of("summary", "draft"), null);
    }

    private LlmTriggerRunRequest componentRequest() {
        return new LlmTriggerRunRequest(
                null, null, null, null, null,
                20L, Map.of("summary", "draft"), "Make it concise");
    }

    private LlmTriggerRunRequest requestWithItem() {
        return new LlmTriggerRunRequest(
                null, null, null, null,
                DATASET_ITEM_ID, 20L, Map.of("summary", "draft"), null);
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
        task.setAiReviewConfigId(AI_REVIEW_CONFIG_ID);
        return task;
    }

    private Assignment assignment() {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTaskId(TASK_ID);
        assignment.setDatasetItemId(DATASET_ITEM_ID);
        assignment.setTemplateVersionId(20L);
        return assignment;
    }

    private DatasetItem datasetItem() {
        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setId(DATASET_ITEM_ID);
        datasetItem.setTaskId(TASK_ID);
        datasetItem.setItemJson("{\"text\":\"raw item\"}");
        return datasetItem;
    }

    private LlmProvider provider() {
        LlmProvider provider = new LlmProvider();
        provider.setId(PROVIDER_ID);
        provider.setEnabled(true);
        provider.setSupportVision(true);
        provider.setSupportMultiImage(true);
        provider.setMaxImageCount(5);
        return provider;
    }

    private AiReviewConfig aiReviewConfig() {
        AiReviewConfig config = new AiReviewConfig();
        config.setId(AI_REVIEW_CONFIG_ID);
        config.setTaskId(TASK_ID);
        config.setProviderId(PROVIDER_ID);
        config.setModelName("qwen-plus");
        config.setScoringDimensionsJson("[\"accuracy\",\"clarity\"]");
        config.setPassThreshold(new BigDecimal("80.00"));
        config.setManualReviewThreshold(new BigDecimal("60.00"));
        config.setPromptVersion("v1");
        config.setMultimodalEnabled(true);
        config.setVisionDetail("auto");
        config.setMaxImagesPerRequest(5);
        return config;
    }

    private TemplateVersion templateVersion() {
        TemplateVersion version = new TemplateVersion();
        version.setId(20L);
        version.setTemplateId(20L);
        version.setTaskId(TASK_ID);
        version.setSchemaJson("""
                {
                  "components": [
                    {
                      "id": "summary",
                      "field": "summary",
                      "type": "TextArea",
                      "label": "Summary"
                    }
                  ]
                }
                """);
        return version;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(AGENT_RUN_ID);
        return run;
    }

    private VideoKeyFrameService videoKeyFrameService() {
        VideoKeyFrameService service = new VideoKeyFrameService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "maxFrames", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "intervalSeconds", 5);
        return service;
    }

    private LlmTriggerRun triggerRun() {
        LlmTriggerRun run = new LlmTriggerRun();
        run.setId(TRIGGER_RUN_ID);
        run.setTaskId(TASK_ID);
        run.setAssignmentId(ASSIGNMENT_ID);
        run.setTemplateVersionId(20L);
        run.setDatasetItemId(DATASET_ITEM_ID);
        run.setProviderId(PROVIDER_ID);
        run.setModelName("qwen-plus");
        run.setAgentRunId(AGENT_RUN_ID);
        run.setStatus(LlmTaskStatus.RUNNING.name());
        run.setComponentId("20");
        run.setTargetFieldsJson("[\"summary\"]");
        run.setInputSnapshotJson("{\"promptTemplate\":\"Suggest a concise summary.\"}");
        run.setCreatedBy(OWNER_ID);
        return run;
    }
}
