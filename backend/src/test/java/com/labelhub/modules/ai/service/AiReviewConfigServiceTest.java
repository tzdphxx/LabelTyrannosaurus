package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.AiReviewPromptTestRequest;
import com.labelhub.modules.ai.dto.AiReviewPromptTestResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewConfigServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long TASK_ID = 10L;
    private static final Long CONFIG_ID = 20L;
    private static final Long PROVIDER_ID = 30L;

    @Mock
    private AiReviewConfigMapper aiReviewConfigMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private LlmProviderService llmProviderService;

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AgentRunService agentRunService;

    @Mock
    private AuditAppender auditAppender;

    @Mock
    private TraceIdProvider traceIdProvider;

    private AiReviewConfigService service;

    @BeforeEach
    void setUp() {
        service = new AiReviewConfigService(
                aiReviewConfigMapper,
                taskMapper,
                llmProviderService,
                llmGateway,
                agentRunService,
                auditAppender,
                traceIdProvider
        );
    }

    @Test
    void createsConfigForDraftTaskAndBackfillsTaskReference() {
        Task task = draftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(aiReviewConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(aiReviewConfigMapper.insert(any(AiReviewConfig.class))).thenAnswer(invocation -> {
            AiReviewConfig config = invocation.getArgument(0);
            config.setId(CONFIG_ID);
            return 1;
        });

        AiReviewConfigResponse response = service.save(OWNER_ID, TASK_ID, request());

        assertThat(response.id()).isEqualTo(CONFIG_ID);
        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.promptVersion()).isEqualTo("v1");
        assertThat(response.scoringDimensions()).containsExactly("accuracy", "safety");
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAiReviewConfigId()).isEqualTo(CONFIG_ID);
        verify(auditAppender).append(eq("AI_REVIEW_CONFIG"), eq(CONFIG_ID), eq("USER"), eq(OWNER_ID),
                eq("AI_REVIEW_CONFIG_CREATED"), eq(null), any(), eq("trace-1"), eq(null));
    }

    @Test
    void rejectsDisabledProvider() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(draftTask());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(OWNER_ID, TASK_ID, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400401));
    }

    @Test
    void rejectsInvalidThresholdOrder() {
        AiReviewConfigRequest invalid = new AiReviewConfigRequest(
                PROVIDER_ID,
                "qwen-plus",
                "Review {{answer}}",
                List.of("accuracy"),
                new BigDecimal("60.00"),
                new BigDecimal("80.00"),
                Map.of("type", "object")
        );
        when(taskMapper.selectById(TASK_ID)).thenReturn(draftTask());

        assertThatThrownBy(() -> service.save(OWNER_ID, TASK_ID, invalid))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400402));
    }

    @Test
    void rejectsNonOwnerTask() {
        Task task = draftTask();
        task.setOwnerId(99L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> service.save(OWNER_ID, TASK_ID, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404001));
    }

    @Test
    void updatesConfigAndIncrementsPromptVersion() {
        AiReviewConfig existing = config();
        existing.setPromptVersion("v3");
        when(taskMapper.selectById(TASK_ID)).thenReturn(draftTask());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(existing);
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(aiReviewConfigMapper.updateById(any(AiReviewConfig.class))).thenReturn(1);

        AiReviewConfigResponse response = service.update(OWNER_ID, TASK_ID, CONFIG_ID, request());

        assertThat(response.promptVersion()).isEqualTo("v4");
        assertThat(existing.getPromptVersion()).isEqualTo("v4");
        verify(auditAppender).append(eq("AI_REVIEW_CONFIG"), eq(CONFIG_ID), eq("USER"), eq(OWNER_ID),
                eq("AI_REVIEW_CONFIG_UPDATED"), any(), any(), eq(null), eq(null));
    }

    @Test
    void promptTestCompletesAgentRunWhenGatewaySucceeds() {
        AiReviewConfig config = config();
        AgentRun run = agentRun(70L);
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("decision", "PASS");
        when(taskMapper.selectById(TASK_ID)).thenReturn(draftTask());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config);
        when(agentRunService.create(eq("AI_REVIEW_CONFIG_TEST"), eq(null), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(run);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"decision\":\"PASS\"}",
                "PASS",
                structured,
                123L,
                null,
                null
        ));

        AiReviewPromptTestResponse response = service.testPrompt(OWNER_ID, TASK_ID, CONFIG_ID, testRequest());

        assertThat(response.agentRunId()).isEqualTo(70L);
        assertThat(response.status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        assertThat(response.structuredJson()).containsEntry("decision", "PASS");
        verify(agentRunService).start(70L);
        verify(agentRunService).complete(eq(70L), any());
    }

    @Test
    void promptTestFailsAgentRunWhenGatewayFails() {
        AiReviewConfig config = config();
        AgentRun run = agentRun(70L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(draftTask());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config);
        when(agentRunService.create(eq("AI_REVIEW_CONFIG_TEST"), eq(null), eq(PROVIDER_ID), eq("qwen-plus"),
                eq("v2"), any())).thenReturn(run);
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.TIMEOUT,
                null,
                null,
                Map.of(),
                3000L,
                "TIMEOUT",
                "Provider timed out"
        ));

        AiReviewPromptTestResponse response = service.testPrompt(OWNER_ID, TASK_ID, CONFIG_ID, testRequest());

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.TIMEOUT);
        assertThat(response.errorMessage()).isEqualTo("Provider timed out");
        verify(agentRunService).fail(70L, AgentRunStatus.FAILED, "Provider timed out");
    }

    private AiReviewConfigRequest request() {
        return new AiReviewConfigRequest(
                PROVIDER_ID,
                "qwen-plus",
                "Review this answer and return JSON.",
                List.of("accuracy", "safety"),
                new BigDecimal("85.00"),
                new BigDecimal("60.00"),
                Map.of("type", "object", "required", List.of("decision"))
        );
    }

    private AiReviewPromptTestRequest testRequest() {
        return new AiReviewPromptTestRequest(
                Map.of("question", "2+2?"),
                Map.of("answer", "4")
        );
    }

    private Task draftTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
        task.setStatus(TaskStatus.DRAFT);
        return task;
    }

    private LlmProvider provider() {
        LlmProvider provider = new LlmProvider();
        provider.setId(PROVIDER_ID);
        provider.setEnabled(true);
        provider.setDefaultModel("qwen-plus");
        return provider;
    }

    private AiReviewConfig config() {
        AiReviewConfig config = new AiReviewConfig();
        config.setId(CONFIG_ID);
        config.setTaskId(TASK_ID);
        config.setProviderId(PROVIDER_ID);
        config.setModelName("qwen-plus");
        config.setPromptTemplate("Review this answer and return JSON.");
        config.setScoringDimensionsJson("[\"accuracy\",\"safety\"]");
        config.setPassThreshold(new BigDecimal("85.00"));
        config.setManualReviewThreshold(new BigDecimal("60.00"));
        config.setOutputSchemaJson("{\"type\":\"object\"}");
        config.setPromptVersion("v2");
        config.setCreatedBy(OWNER_ID);
        return config;
    }

    private AgentRun agentRun(Long id) {
        AgentRun run = new AgentRun();
        run.setId(id);
        return run;
    }
}
