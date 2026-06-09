package com.labelhub.modules.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.PageResponse;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.TaskStatisticsResponse;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskManagementServiceTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskTagMapper taskTagMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private DatasetItemMapper datasetItemMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private AuditAppender auditAppender;
    @Mock private TraceIdProvider traceIdProvider;

    @Test
    void listOwnerTasksPageLoadsTagsInBatch() {
        Task first = task(10L, "A");
        Task second = task(11L, "B");
        TaskTag tagA = tag(10L, "image");
        TaskTag tagB = tag(11L, "text");
        when(taskMapper.countOwnerTasks(1L, null, null)).thenReturn(2L);
        when(taskMapper.selectOwnerTasksPage(1L, null, null, 20, 0))
                .thenReturn(List.of(first, second));
        when(taskTagMapper.selectByTaskIds(List.of(10L, 11L)))
                .thenReturn(List.of(tagA, tagB));

        PageResponse<TaskSummaryResponse> response = service().listOwnerTasksPage(
                1L, null, null, 1, 20);

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.items().get(0).tags()).containsExactly("image");
        assertThat(response.items().get(1).tags()).containsExactly("text");
        verify(taskTagMapper).selectByTaskIds(List.of(10L, 11L));
    }

    @Test
    void getStatisticsUsesAggregatedStatusCounts() {
        Task task = task(10L, "A");
        task.setOwnerId(1L);
        task.setClaimedCount(5);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(datasetItemMapper.countByTaskId(10L)).thenReturn(20);
        when(submissionMapper.selectStatusCountsByTaskId(10L)).thenReturn(List.of(
                Map.of("status", "PENDING_FINAL", "count", 3),
                Map.of("status", "APPROVED", "count", 6),
                Map.of("status", "REJECTED", "count", 2)));

        TaskStatisticsResponse response = service().getStatistics(1L, 10L);

        assertThat(response.submittedCount()).isEqualTo(11);
        assertThat(response.pendingReviewCount()).isEqualTo(3);
        assertThat(response.approvedCount()).isEqualTo(6);
        assertThat(response.rejectedCount()).isEqualTo(2);
        assertThat(response.passRate()).isEqualTo("75.00%");
        verify(submissionMapper, never()).countByTaskIdAndStatus(10L, "PENDING_FINAL");
    }

    private TaskManagementService service() {
        return new TaskManagementService(taskMapper, taskTagMapper, assignmentMapper,
                datasetItemMapper, submissionMapper, auditAppender, traceIdProvider);
    }

    private Task task(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setOwnerId(1L);
        task.setTitle(title);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setQuota(100);
        task.setClaimedCount(0);
        task.setOverlapCount(1);
        return task;
    }

    private TaskTag tag(Long taskId, String name) {
        TaskTag tag = new TaskTag();
        tag.setTaskId(taskId);
        tag.setTagName(name);
        return tag;
    }
}
