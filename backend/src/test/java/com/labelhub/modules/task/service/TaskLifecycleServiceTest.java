package com.labelhub.modules.task.service;

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
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
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

    private TaskLifecycleService taskLifecycleService;

    @BeforeEach
    void setUp() {
        taskLifecycleService = new TaskLifecycleService(
                taskMapper,
                taskTagMapper,
                publishDependencyChecker,
                auditAppender,
                traceIdProvider
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
        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_CREATED"),
                eq(null), any(), eq("trace-1"), eq(null));
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
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(taskTag("qa")));

        TaskDetailResponse response = taskLifecycleService.getOwnedTask(OWNER_ID, TASK_ID);

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.tags()).containsExactly("qa");
        assertThat(response.publishedTemplateVersionId()).isEqualTo(100L);
        assertThat(response.aiReviewConfigId()).isEqualTo(200L);
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
        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_UPDATED"),
                any(), any(), eq(null), eq(null));
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

        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_PUBLISHED"),
                any(), any(), eq(null), eq(null));
        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_PAUSED"),
                any(), any(), eq(null), eq(null));
        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_RESUMED"),
                any(), any(), eq(null), eq(null));
        verify(auditAppender).append(eq("TASK"), eq(TASK_ID), eq("USER"), eq(OWNER_ID), eq("TASK_ENDED"),
                any(), any(), eq(null), eq(null));
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
    void defaultPublishDependencyCheckerDoesNotPassExternalChecks() {
        AiReviewConfigMapper aiReviewConfigMapper = Mockito.mock(AiReviewConfigMapper.class);
        DefaultTaskPublishDependencyChecker checker = new DefaultTaskPublishDependencyChecker(aiReviewConfigMapper);

        assertThat(checker.datasetReady(TASK_ID)).isFalse();
        assertThat(checker.templateVersionExists(100L)).isFalse();
        assertThat(checker.aiReviewConfigExists(TASK_ID, 200L)).isFalse();
        assertThat(checker.rewardRuleExists(TASK_ID)).isFalse();
    }

    private CreateTaskRequest createRequest() {
        return new CreateTaskRequest(
                "New task",
                "Description",
                "Instruction",
                List.of("qa"),
                10,
                LocalDateTime.now().plusDays(1),
                1,
                100L,
                200L
        );
    }

    private UpdateTaskRequest updateRequest() {
        return new UpdateTaskRequest(
                "Updated task",
                "Updated description",
                "Updated instruction",
                List.of("review"),
                20,
                LocalDateTime.now().plusDays(2),
                2,
                100L,
                200L
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
