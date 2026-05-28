package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmTriggerServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long LABELER_ID = 2L;
    private static final Long TASK_ID = 10L;
    private static final Long TEMPLATE_VERSION_ID = 20L;
    private static final Long DATASET_ITEM_ID = 30L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long PROVIDER_ID = 50L;
    private static final Long AGENT_RUN_ID = 60L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TemplateVersionMapper templateVersionMapper;

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

    private LlmTriggerService service;

    @BeforeEach
    void setUp() {
        service = new LlmTriggerService(taskMapper, templateVersionMapper, datasetItemMapper, assignmentMapper,
                llmProviderService, rateLimiter, llmGateway, agentRunService, auditAppender, traceIdProvider);
    }

    @Test
    void ownerPreviewRunsLlmTriggerAndReturnsSuggestion() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(templateVersion());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), eq(null), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("component:assist-summary"), any())).thenReturn(agentRun());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"suggestion\":\"Looks good\"}",
                "Looks good",
                Map.of("suggestion", "Looks good"),
                42L,
                null,
                null
        ));

        LlmTriggerRunResponse response = service.run(owner(), request(true, null));

        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        assertThat(response.componentId()).isEqualTo("assist-summary");
        assertThat(response.suggestionJson()).containsEntry("suggestion", "Looks good");
        assertThat(response.targetFields()).containsExactly("summary");
        verify(agentRunService).start(AGENT_RUN_ID);
        verify(agentRunService).complete(eq(AGENT_RUN_ID), any());
        verify(auditAppender).append(eq("LLM_TRIGGER"), eq(TASK_ID), eq("USER"), eq(OWNER_ID),
                eq("LLM_TRIGGER_RUN"), eq(null), any(), eq("trace-1"), eq(AGENT_RUN_ID));
    }

    @Test
    void labelerWorkbenchRequiresOwnedAssignment() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(templateVersion());
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.run(labeler(), request(false, ASSIGNMENT_ID)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void rejectsNonLlmTriggerComponent() {
        TemplateVersion templateVersion = templateVersion("""
                {"components":[{"id":"text-1","type":"Textarea"}]}
                """);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(templateVersion);
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());

        assertThatThrownBy(() -> service.run(owner(), request(true, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400501));
    }

    @Test
    void marksAgentRunFailedWhenGatewayFails() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(templateVersion());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), eq(null), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("component:assist-summary"), any())).thenReturn(agentRun());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT,
                null,
                null,
                Map.of(),
                3000L,
                "TIMEOUT",
                "Provider timed out"
        ));

        LlmTriggerRunResponse response = service.run(owner(), request(true, null));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.TIMEOUT);
        assertThat(response.errorMessage()).isEqualTo("Provider timed out");
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.FAILED, "Provider timed out");
    }

    @Test
    void rateLimitedCallMarksAgentRunAndSkipsGateway() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(templateVersion());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), eq(null), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("component:assist-summary"), any())).thenReturn(agentRun());
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(false);

        LlmTriggerRunResponse response = service.run(owner(), request(true, null));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.RATE_LIMITED);
        assertThat(response.errorCode()).isEqualTo("RATE_LIMITED");
        verify(agentRunService).fail(AGENT_RUN_ID, AgentRunStatus.RATE_LIMITED, "LLM trigger rate limited");
    }

    private CurrentUser owner() {
        return new CurrentUser(OWNER_ID, "owner", Set.of(RoleCode.OWNER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(LABELER_ID, "labeler", Set.of(RoleCode.LABELER), 1);
    }

    private LlmTriggerRunRequest request(boolean previewMode, Long assignmentId) {
        return new LlmTriggerRunRequest(
                TASK_ID,
                TEMPLATE_VERSION_ID,
                "assist-summary",
                DATASET_ITEM_ID,
                assignmentId,
                Map.of("summary", "draft"),
                previewMode
        );
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
        return task;
    }

    private TemplateVersion templateVersion() {
        return templateVersion("""
                {
                  "components": [
                    {
                      "id": "assist-summary",
                      "type": "LlmTrigger",
                      "providerId": 50,
                      "modelName": "qwen-plus",
                      "promptTemplate": "Suggest a concise summary.",
                      "targetFields": ["summary"]
                    }
                  ]
                }
                """);
    }

    private TemplateVersion templateVersion(String schemaJson) {
        TemplateVersion templateVersion = new TemplateVersion();
        templateVersion.setId(TEMPLATE_VERSION_ID);
        templateVersion.setTaskId(TASK_ID);
        templateVersion.setSchemaJson(schemaJson);
        return templateVersion;
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
        return provider;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(AGENT_RUN_ID);
        return run;
    }
}
