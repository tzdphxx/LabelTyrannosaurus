package com.labelhub.modules.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.assignment.mapper.AssignmentDispatchMapper;
import com.labelhub.modules.ai.service.AiReviewConfigService;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.reward.repository.RewardRuleRepositoryMapper;
import com.labelhub.modules.reward.service.RewardRuleService;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskResponse;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.OwnerTaskSummaryResponse;
import com.labelhub.modules.task.dto.TaskDetailResponse;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskLifecycleServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long TASK_ID = 10L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskTagMapper taskTagMapper;

    @Mock
    private TaskPublishDependencyChecker publishDependencyChecker;

    @Mock
    private AuditAppender auditAppender;

    @Mock
    private TraceIdProvider traceIdProvider;

    @Mock
    private DatasetImportService datasetImportService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AiReviewConfigService aiReviewConfigService;

    @Mock
    private RewardRuleService rewardRuleService;

    @Mock
    private AssignmentDispatchMapper dispatchMapper;

    private TaskLifecycleService taskLifecycleService;

    @BeforeEach
    void setUp() {
        taskLifecycleService = new TaskLifecycleService(
                taskMapper,
                taskTagMapper,
                publishDependencyChecker,
                auditAppender,
                traceIdProvider,
                datasetImportService,
                aiReviewConfigService,
                rewardRuleService,
                dispatchMapper,
                applicationEventPublisher
        );
    }

    @Test
    void createsDraftTaskWithTagsAndAudit() {
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        });

        TaskLifecycleResponse response = taskLifecycleService.create(OWNER_ID, createRequest());

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.DRAFT);
        verify(taskTagMapper).insert(any(TaskTag.class));
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void acceptsTaskCreationWithOverlapCount() {
        // overlapCount validation is now handled by @Max(1) on CreateTaskRequest DTO at controller layer
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        });

        TaskLifecycleResponse response = taskLifecycleService.create(OWNER_ID, createRequestWithOverlapCount(2));

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        verify(taskMapper).insert(any(Task.class));
    }

    @Test
    void createsDraftTaskAndStartsDatasetImportWhenFileProvided() {
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        });
        DatasetImportJobResponse importJob = new DatasetImportJobResponse(
                300L, TASK_ID, "PENDING", "APPEND", 0, 0, 0,
                null, null, null, null, null, null
        );
        when(datasetImportService.createAppendImport(
                eq(TASK_ID),
                eq(new DatasetImportRequest(99L))
        )).thenReturn(importJob);

        CreateTaskResponse response = taskLifecycleService.createWithDataset(OWNER_ID, createRequestWithDataset());

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        assertThat(response.datasetImportJob()).isEqualTo(importJob);
        verify(datasetImportService).createAppendImport(TASK_ID, new DatasetImportRequest(99L));
    }

    @Test
    void createsTaskWithoutCheckingTemplateVersionOwnership() {
        // template version ownership is validated during publish, not during create
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        });

        TaskLifecycleResponse response = taskLifecycleService.create(OWNER_ID, createRequest());

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        verify(taskMapper).insert(any(Task.class));
    }

    @Test
    void createsDraftTaskWithRewardRule() {
        RewardRuleRequest rewardRule = rewardRuleRequest("2.50", true);
        RewardRuleResponse rewardResponse = rewardRuleResponse(1, "2.50", true);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-1");
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        });
        when(rewardRuleService.saveRuleForTaskOwner(TASK_ID, OWNER_ID, rewardRule)).thenReturn(rewardResponse);

        CreateTaskResponse response = taskLifecycleService.createWithDataset(OWNER_ID, createRequestWithRewardRule(rewardRule));

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.rewardRule()).isEqualTo(rewardResponse);
        verify(rewardRuleService).saveRuleForTaskOwner(TASK_ID, OWNER_ID, rewardRule);
    }

    @Test
    void listsOwnerTasksWithTags() {
        Task task = draftTask();
        task.setClaimedCount(0);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(task));
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(taskTag("qa")));

        List<OwnerTaskSummaryResponse> responses = taskLifecycleService.listOwnerTasks(OWNER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).taskId()).isEqualTo(TASK_ID);
        assertThat(responses.get(0).tags()).containsExactly("qa");
    }

    @Test
    void returnsOwnedTaskDetail() {
        Task task = publishableDraftTask();
        RewardRuleResponse rewardRule = rewardRuleResponse(2, "3.00", false);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(taskTag("qa")));
        when(rewardRuleService.findLatestRule(TASK_ID)).thenReturn(rewardRule);

        TaskDetailResponse response = taskLifecycleService.getOwnedTask(OWNER_ID, TASK_ID);

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.tags()).containsExactly("qa");
        assertThat(response.publishedTemplateVersionId()).isEqualTo(100L);
        assertThat(response.aiReviewConfigId()).isEqualTo(200L);
        assertThat(response.rewardRule()).isEqualTo(rewardRule);
    }

    @Test
    void returnsOwnedTaskDetailWithoutRewardRule() {
        Task task = publishableDraftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(taskTag("qa")));
        when(rewardRuleService.findLatestRule(TASK_ID)).thenReturn(null);

        TaskDetailResponse response = taskLifecycleService.getOwnedTask(OWNER_ID, TASK_ID);

        assertThat(response.rewardRule()).isNull();
    }

    @Test
    void updatesDraftTaskOnly() {
        Task task = draftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskTagMapper.delete(any(Wrapper.class))).thenReturn(1);

        TaskLifecycleResponse response = taskLifecycleService.updateDraft(OWNER_ID, TASK_ID, updateRequest());

        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        assertThat(task.getTitle()).isEqualTo("Updated task");
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void updatesDraftTaskWithRewardRule() {
        Task task = draftTask();
        RewardRuleRequest rewardRule = rewardRuleRequest("4.00", false);
        RewardRuleResponse rewardResponse = rewardRuleResponse(3, "4.00", false);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskTagMapper.delete(any(Wrapper.class))).thenReturn(1);
        when(rewardRuleService.saveRuleForTaskOwner(TASK_ID, OWNER_ID, rewardRule)).thenReturn(rewardResponse);

        TaskLifecycleResponse response = taskLifecycleService.updateDraft(OWNER_ID, TASK_ID, updateRequestWithRewardRule(rewardRule));

        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        assertThat(task.getRewardVisible()).isFalse();
        verify(rewardRuleService).saveRuleForTaskOwner(TASK_ID, OWNER_ID, rewardRule);
    }

    @Test
    void acceptsDraftUpdateWithOverlapCount() {
        // overlapCount validation is now handled by @Max(1) on UpdateTaskRequest DTO at controller layer
        Task task = draftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskTagMapper.delete(any(Wrapper.class))).thenReturn(1);

        TaskLifecycleResponse response = taskLifecycleService.updateDraft(OWNER_ID, TASK_ID, updateRequestWithOverlapCount(2));

        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        verify(taskMapper).updateById(any(Task.class));
    }

    @Test
    void updatesDraftWithoutCheckingTemplateVersionOwnership() {
        // template version ownership is validated during publish, not during update
        Task task = draftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskTagMapper.delete(any(Wrapper.class))).thenReturn(1);

        TaskLifecycleResponse response = taskLifecycleService.updateDraft(OWNER_ID, TASK_ID, updateRequest());

        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        verify(taskMapper).updateById(any(Task.class));
    }

    @Test
    void rejectsEditingNonDraftTask() {
        Task task = draftTask();
        task.setStatus(TaskStatus.PUBLISHED);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> taskLifecycleService.updateDraft(OWNER_ID, TASK_ID, updateRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400101));
    }

    @Test
    void transitionsThroughLifecycle() {
        Task task = publishableDraftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(publishDependencyChecker.datasetReady(TASK_ID)).thenReturn(true);
        when(publishDependencyChecker.templateVersionExists(100L)).thenReturn(true);
        when(publishDependencyChecker.aiReviewConfigExists(TASK_ID, 200L)).thenReturn(true);
        when(publishDependencyChecker.rewardRuleExists(TASK_ID)).thenReturn(true);

        assertThat(taskLifecycleService.publish(OWNER_ID, TASK_ID).status()).isEqualTo(TaskStatus.PUBLISHED);
        assertThat(taskLifecycleService.pause(OWNER_ID, TASK_ID).status()).isEqualTo(TaskStatus.PAUSED);
        assertThat(taskLifecycleService.resume(OWNER_ID, TASK_ID).status()).isEqualTo(TaskStatus.PUBLISHED);
        assertThat(taskLifecycleService.end(OWNER_ID, TASK_ID).status()).isEqualTo(TaskStatus.ENDED);

        verify(auditAppender, Mockito.times(4)).append(any(AuditCommand.class));
    }

    @Test
    void rejectsIllegalTransition() {
        Task task = draftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> taskLifecycleService.pause(OWNER_ID, TASK_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400101));
    }

    @Test
    void rejectsPublishWhenRequirementMissing() {
        Task task = publishableDraftTask();
        task.setAiReviewConfigId(null);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(publishDependencyChecker.datasetReady(TASK_ID)).thenReturn(true);
        when(publishDependencyChecker.templateVersionExists(100L)).thenReturn(true);

        assertThatThrownBy(() -> taskLifecycleService.publish(OWNER_ID, TASK_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400102));
    }

    @Test
    void rejectsPublishWhenTemplateVersionDoesNotExist() {
        // publish validates templateVersionExists, not templateVersionUsableByTask (removed)
        Task task = publishableDraftTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(publishDependencyChecker.datasetReady(TASK_ID)).thenReturn(true);
        when(publishDependencyChecker.templateVersionExists(100L)).thenReturn(false);

        assertThatThrownBy(() -> taskLifecycleService.publish(OWNER_ID, TASK_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400102));
    }

    @Test
    void defaultPublishDependencyCheckerDoesNotPassExternalChecks() {
        TaskMapper dependencyTaskMapper = Mockito.mock(TaskMapper.class);
        AiReviewConfigMapper aiReviewConfigMapper = Mockito.mock(AiReviewConfigMapper.class);
        DatasetItemMapper datasetItemMapper = Mockito.mock(DatasetItemMapper.class);
        TemplateVersionMapper templateVersionMapper = Mockito.mock(TemplateVersionMapper.class);
        RewardRuleRepositoryMapper rewardRuleRepoMapper = Mockito.mock(RewardRuleRepositoryMapper.class);
        DefaultTaskPublishDependencyChecker checker = new DefaultTaskPublishDependencyChecker(
                dependencyTaskMapper, aiReviewConfigMapper, datasetItemMapper, templateVersionMapper, rewardRuleRepoMapper);

        assertThat(checker.datasetReady(TASK_ID)).isFalse();
        assertThat(checker.templateVersionOwnedBy(OWNER_ID, 100L)).isFalse();
        assertThat(checker.templateVersionUsableByTask(TASK_ID, 100L)).isFalse();
        assertThat(checker.aiReviewConfigExists(TASK_ID, 200L)).isFalse();
        assertThat(checker.rewardRuleExists(TASK_ID)).isFalse();
    }

    @Test
    void defaultPublishDependencyCheckerAllowsOnlyOwnerTemplateVersion() {
        TaskMapper dependencyTaskMapper = Mockito.mock(TaskMapper.class);
        AiReviewConfigMapper aiReviewConfigMapper = Mockito.mock(AiReviewConfigMapper.class);
        DatasetItemMapper datasetItemMapper = Mockito.mock(DatasetItemMapper.class);
        TemplateVersionMapper templateVersionMapper = Mockito.mock(TemplateVersionMapper.class);
        RewardRuleRepositoryMapper rewardRuleRepoMapper = Mockito.mock(RewardRuleRepositoryMapper.class);
        DefaultTaskPublishDependencyChecker checker = new DefaultTaskPublishDependencyChecker(
                dependencyTaskMapper, aiReviewConfigMapper, datasetItemMapper, templateVersionMapper, rewardRuleRepoMapper);
        Task task = publishableDraftTask();
        com.labelhub.modules.template.domain.TemplateVersion version = new com.labelhub.modules.template.domain.TemplateVersion();
        version.setId(100L);
        version.setOwnerId(OWNER_ID);
        when(dependencyTaskMapper.selectById(TASK_ID)).thenReturn(task);
        when(templateVersionMapper.selectById(100L)).thenReturn(version);

        assertThat(checker.templateVersionOwnedBy(OWNER_ID, 100L)).isTrue();
        assertThat(checker.templateVersionUsableByTask(TASK_ID, 100L)).isTrue();

        version.setOwnerId(99L);
        assertThat(checker.templateVersionOwnedBy(OWNER_ID, 100L)).isFalse();
        assertThat(checker.templateVersionUsableByTask(TASK_ID, 100L)).isFalse();
    }

    private CreateTaskRequest createRequest() {
        return createRequestWithOverlapCount(1);
    }

    private CreateTaskRequest createRequestWithOverlapCount(int overlapCount) {
        return new CreateTaskRequest(
                "New task",
                "Description",
                "Instruction",
                List.of("qa"),
                10,
                LocalDateTime.now().plusDays(1),
                overlapCount,
                100L,
                200L,
                null, null, null, null, null, null,
                null,
                null, null,
                1,
                null,
                null
        );
    }

    private CreateTaskRequest createRequestWithDataset() {
        return new CreateTaskRequest(
                "New task",
                "Description",
                "Instruction",
                List.of("qa"),
                10,
                LocalDateTime.now().plusDays(1),
                1,
                100L,
                200L,
                null, null, null, null, null, null,
                null,
                null, null,
                1,
                99L,
                null
        );
    }

    private CreateTaskRequest createRequestWithRewardRule(RewardRuleRequest rewardRule) {
        return new CreateTaskRequest(
                "New task",
                "Description",
                "Instruction",
                List.of("qa"),
                10,
                LocalDateTime.now().plusDays(1),
                1,
                100L,
                200L,
                null, null, null, null, null, null,
                null,
                null, null,
                1,
                null,
                rewardRule
        );
    }

    private UpdateTaskRequest updateRequest() {
        return updateRequestWithOverlapCount(1);
    }

    private UpdateTaskRequest updateRequestWithOverlapCount(int overlapCount) {
        return new UpdateTaskRequest(
                "Updated task",
                "Updated description",
                "Updated instruction",
                List.of("review"),
                20,
                LocalDateTime.now().plusDays(2),
                overlapCount,
                100L,
                200L,
                1,
                null,
                null,
                null
        );
    }

    private UpdateTaskRequest updateRequestWithRewardRule(RewardRuleRequest rewardRule) {
        return new UpdateTaskRequest(
                "Updated task",
                "Updated description",
                "Updated instruction",
                List.of("review"),
                20,
                LocalDateTime.now().plusDays(2),
                1,
                100L,
                200L,
                1,
                null,
                null,
                rewardRule
        );
    }

    private RewardRuleRequest rewardRuleRequest(String unitReward, boolean rewardVisible) {
        return new RewardRuleRequest("APPROVED_ITEM", new java.math.BigDecimal(unitReward), "POINT", rewardVisible);
    }

    private RewardRuleResponse rewardRuleResponse(int version, String unitReward, boolean rewardVisible) {
        return new RewardRuleResponse(
                100L + version,
                TASK_ID,
                version,
                "APPROVED_ITEM",
                new java.math.BigDecimal(unitReward),
                "POINT",
                rewardVisible,
                LocalDateTime.now(),
                OWNER_ID,
                LocalDateTime.now()
        );
    }

    private Task draftTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
        task.setTitle("Draft task");
        task.setStatus(TaskStatus.DRAFT);
        task.setQuota(10);
        task.setOverlapCount(1);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        return task;
    }

    private TaskTag taskTag(String tagName) {
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(TASK_ID);
        taskTag.setTagName(tagName);
        return taskTag;
    }

    private Task publishableDraftTask() {
        Task task = draftTask();
        task.setPublishedTemplateVersionId(100L);
        task.setAiReviewConfigId(200L);
        return task;
    }
}
