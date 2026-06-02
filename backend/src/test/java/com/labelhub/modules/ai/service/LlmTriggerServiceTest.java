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
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskStatus;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.mapper.LlmTriggerRunMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
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
    private static final Long DATASET_ITEM_ID = 30L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long PROVIDER_ID = 50L;
    private static final Long AGENT_RUN_ID = 60L;
    private static final Long TRIGGER_RUN_ID = 70L;

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
    private LlmTriggerRunMapper llmTriggerRunMapper;

    private LlmTriggerService service;

    @BeforeEach
    void setUp() {
        service = new LlmTriggerService(taskMapper, datasetItemMapper, assignmentMapper,
                llmProviderService, rateLimiter, llmGateway, agentRunService, auditAppender, traceIdProvider,
                llmTaskQueueService, llmTriggerRunMapper);
    }

    @Test
    void labelerTriggersFromAssignmentAndEnqueuesRun() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(datasetItemMapper.selectById(DATASET_ITEM_ID)).thenReturn(datasetItem());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), isNull(), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("target:summary"), any(), eq(ASSIGNMENT_ID))).thenReturn(agentRun());
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        doAnswer(invocation -> {
            LlmTriggerRun run = invocation.getArgument(0);
            run.setId(TRIGGER_RUN_ID);
            return 1;
        }).when(llmTriggerRunMapper).insert(any(LlmTriggerRun.class));

        LlmTriggerRunResponse response = service.runForAssignment(labeler(), ASSIGNMENT_ID, request());

        assertThat(response.triggerRunId()).isEqualTo(TRIGGER_RUN_ID);
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        assertThat(response.status()).isEqualTo(LlmTaskStatus.RUNNING.name());
        assertThat(response.targetFields()).containsExactly("summary");
        verify(agentRunService).start(AGENT_RUN_ID);
        verify(llmTaskQueueService).enqueue(any());
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void labelerRequiresOwnedAssignment() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.runForAssignment(labeler(), ASSIGNMENT_ID, request()))
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
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("LLM_TRIGGER"), isNull(), eq(PROVIDER_ID), eq("qwen-plus"),
                any(), any(), isNull())).thenReturn(agentRun());
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        doAnswer(invocation -> {
            LlmTriggerRun run = invocation.getArgument(0);
            run.setId(TRIGGER_RUN_ID);
            return 1;
        }).when(llmTriggerRunMapper).insert(any(LlmTriggerRun.class));

        LlmTriggerRunResponse response = service.testFromTask(owner(), TASK_ID, requestWithItem());

        assertThat(response.triggerRunId()).isEqualTo(TRIGGER_RUN_ID);
    }

    @Test
    void rejectsDisabledProvider() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runForAssignment(labeler(), ASSIGNMENT_ID, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400503));
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
        when(rateLimiter.acquire(TASK_ID, OWNER_ID, PROVIDER_ID)).thenReturn(true);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT, null, null, Map.of(), 3000L, "TIMEOUT", "Provider timed out"));

        service.executeQueuedTrigger(TRIGGER_RUN_ID);

        assertThat(run.getStatus()).isEqualTo(LlmTaskStatus.FAILED.name());
        assertThat(run.getErrorMessage()).isEqualTo("Provider timed out");
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
                null, Map.of("summary", "draft"));
    }

    private LlmTriggerRunRequest requestWithItem() {
        return new LlmTriggerRunRequest(
                PROVIDER_ID, "qwen-plus",
                "Suggest a concise summary.",
                List.of("summary"),
                DATASET_ITEM_ID, Map.of("summary", "draft"));
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
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
        return provider;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(AGENT_RUN_ID);
        return run;
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
        run.setTargetFieldsJson("[\"summary\"]");
        run.setInputSnapshotJson("{\"promptTemplate\":\"Suggest a concise summary.\"}");
        run.setCreatedBy(OWNER_ID);
        return run;
    }
}
