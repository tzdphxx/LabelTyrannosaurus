package com.labelhub.modules.preannotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.service.DefaultMediaPromptContextBuilder;
import com.labelhub.modules.ai.service.LlmProviderService;
import com.labelhub.modules.ai.service.ProviderCapability;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.dto.PreAnnotationRunRequest;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class PreAnnotationServiceTest {

    private static final Long ASSIGNMENT_ID = 10L;
    private static final Long LABELER_ID = 20L;
    private static final Long TASK_ID = 30L;
    private static final Long CONFIG_ID = 40L;
    private static final Long PROVIDER_ID = 50L;
    private static final Long AGENT_RUN_ID = 60L;

    @Mock private AssignmentMapper assignmentMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private DatasetItemMapper datasetItemMapper;
    @Mock private TemplateVersionMapper templateVersionMapper;
    @Mock private AiReviewConfigMapper aiReviewConfigMapper;
    @Mock private LlmProviderService llmProviderService;
    @Mock private LlmGateway llmGateway;
    @Mock private AgentRunService agentRunService;
    @Mock private PreAnnotationMapper preAnnotationMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private AuditAppender auditAppender;
    @Mock private TraceIdProvider traceIdProvider;

    private PreAnnotationService service;

    @BeforeEach
    void setUp() {
        service = new PreAnnotationService(
                assignmentMapper, taskMapper, datasetItemMapper, templateVersionMapper, aiReviewConfigMapper,
                llmProviderService, llmGateway, agentRunService, preAnnotationMapper, submissionMapper,
                auditAppender, traceIdProvider, new DefaultMediaPromptContextBuilder());
    }

    @Test
    void runStoresSuggestionWithoutMutatingAssignmentOrSubmission() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(datasetItemMapper.selectById(70L)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(80L)).thenReturn(templateVersion());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(agentRunService.create(eq("PRE_ANNOTATION"), eq(null), eq(PROVIDER_ID), eq("qwen-vl"),
                eq("v1"), any(), eq(ASSIGNMENT_ID))).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"ok\":true}",
                "{\"suggestedAnswerJson\":{\"label\":\"cat\"},\"fieldSuggestions\":[{\"field\":\"label\"}],\"riskFlags\":[],\"overallConfidence\":0.86,\"limitations\":[]}",
                Map.of(
                        "suggestedAnswerJson", Map.of("label", "cat"),
                        "fieldSuggestions", List.of(Map.of("field", "label")),
                        "riskFlags", List.of(),
                        "overallConfidence", 0.86,
                        "limitations", List.of()
                ),
                100L,
                null,
                null
        ));

        PreAnnotationResponse response = service.run(ASSIGNMENT_ID, LABELER_ID, null);

        assertThat(response.status()).isEqualTo(PreAnnotationStatus.SUCCESS);
        assertThat(response.suggestedAnswerJson()).containsEntry("label", "cat");
        verify(agentRunService).complete(eq(AGENT_RUN_ID), any());
        verify(preAnnotationMapper).insert(any(PreAnnotation.class));
        verify(assignmentMapper, never()).updateById(any(Assignment.class));
        verify(submissionMapper, never()).insert(any(Submission.class));
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void runPersistsPendingThenRunningThenSuccessAndFiltersIllegalFields() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task());
        when(aiReviewConfigMapper.selectById(CONFIG_ID)).thenReturn(config());
        when(datasetItemMapper.selectById(70L)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(80L)).thenReturn(templateVersion());
        when(llmProviderService.findEnabledById(PROVIDER_ID)).thenReturn(Optional.of(provider()));
        when(llmProviderService.capability(any(LlmProvider.class))).thenReturn(
                new ProviderCapability(true, true, 10, null));
        when(agentRunService.create(eq("PRE_ANNOTATION"), eq(null), eq(PROVIDER_ID), eq("qwen-vl"),
                eq("v1"), any(), eq(ASSIGNMENT_ID))).thenReturn(agentRun());
        when(llmGateway.review(any(LlmGatewayRequest.class))).thenReturn(new LlmGatewayResponse(
                LlmGatewayStatus.SUCCESS,
                "{\"ok\":true}",
                "{\"suggestedAnswerJson\":{\"label\":\"cat\",\"showOnly\":\"no\"},\"fieldSuggestions\":[{\"field\":\"label\"},{\"field\":\"showOnly\"}],\"riskFlags\":[],\"overallConfidence\":0.86,\"limitations\":[]}",
                Map.of(
                        "suggestedAnswerJson", Map.of("label", "cat", "showOnly", "no"),
                        "fieldSuggestions", List.of(Map.of("field", "label"), Map.of("field", "showOnly")),
                        "riskFlags", List.of(),
                        "overallConfidence", 0.86,
                        "limitations", List.of()
                ),
                100L,
                null,
                null
        ));

        List<PreAnnotationStatus> insertStatuses = new ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            PreAnnotation pa = inv.getArgument(0);
            insertStatuses.add(pa.getStatus());
            return 1;
        }).when(preAnnotationMapper).insert(any(PreAnnotation.class));

        List<PreAnnotationStatus> updateStatuses = new ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            PreAnnotation pa = inv.getArgument(0);
            updateStatuses.add(pa.getStatus());
            return 1;
        }).when(preAnnotationMapper).updateById(any(PreAnnotation.class));

        PreAnnotationResponse response = service.run(ASSIGNMENT_ID, LABELER_ID, new PreAnnotationRunRequest(
                80L,
                70L,
                "{\"label\":\"draft\"}",
                "SUGGEST_ONLY"
        ));

        assertThat(insertStatuses).containsExactly(PreAnnotationStatus.PENDING);
        assertThat(updateStatuses).contains(PreAnnotationStatus.RUNNING, PreAnnotationStatus.SUCCESS);
        assertThat(response.suggestedAnswerJson()).containsEntry("label", "cat");
        assertThat(response.suggestedAnswerJson()).doesNotContainKey("showOnly");
        assertThat(response.ignoredFields()).contains("showOnly");
        assertThat(response.mediaUnderstanding()).containsEntry("usedMedia", true);
    }

    @Test
    void rejectsAssignmentOwnedByAnotherLabeler() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.run(ASSIGNMENT_ID, LABELER_ID, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403801));
    }

    private Assignment assignment() {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTaskId(TASK_ID);
        assignment.setDatasetItemId(70L);
        assignment.setLabelerId(LABELER_ID);
        assignment.setTemplateVersionId(80L);
        assignment.setStatus(AssignmentStatus.DRAFTING);
        return assignment;
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setAiReviewConfigId(CONFIG_ID);
        return task;
    }

    private AiReviewConfig config() {
        AiReviewConfig config = new AiReviewConfig();
        config.setId(CONFIG_ID);
        config.setTaskId(TASK_ID);
        config.setProviderId(PROVIDER_ID);
        config.setModelName("qwen-vl");
        config.setPromptTemplate("Suggest answer");
        config.setPromptVersion("v1");
        config.setMultimodalEnabled(true);
        config.setVisionDetail("auto");
        config.setMaxImagesPerRequest(5);
        config.setDegradationPenalty(new BigDecimal("0.20"));
        return config;
    }

    private DatasetItem datasetItem() {
        DatasetItem item = new DatasetItem();
        item.setId(70L);
        item.setItemJson("{\"media_type\":\"image\",\"media_url\":\"https://e.com/cat.jpg\"}");
        return item;
    }

    private TemplateVersion templateVersion() {
        TemplateVersion version = new TemplateVersion();
        version.setId(80L);
        version.setSchemaJson("""
                {"components":[
                  {"field":"label","type":"Input"},
                  {"field":"showOnly","type":"ShowItem"}
                ]}
                """);
        return version;
    }

    private LlmProvider provider() {
        LlmProvider provider = new LlmProvider();
        provider.setId(PROVIDER_ID);
        provider.setEnabled(true);
        provider.setSupportVision(true);
        provider.setSupportMultiImage(true);
        provider.setMaxImageCount(10);
        return provider;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(AGENT_RUN_ID);
        return run;
    }
}
