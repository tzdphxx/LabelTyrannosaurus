package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.assignment.dto.RewardSummaryResponse;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskMarketServiceTest {

    private static final Long LABELER_ID = 20L;
    private static final Long TASK_ID = 10L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskTagMapper taskTagMapper;

    @Mock
    private DatasetMarketStatsService datasetMarketStatsService;

    @Mock
    private AssignmentMarketStatsService assignmentMarketStatsService;

    @Mock
    private RewardSummaryService rewardSummaryService;

    private TaskMarketService taskMarketService;

    @BeforeEach
    void setUp() {
        taskMarketService = new TaskMarketService(
                taskMapper,
                taskTagMapper,
                datasetMarketStatsService,
                assignmentMarketStatsService,
                rewardSummaryService
        );
    }

    @Test
    void listsPublishedUnexpiredTasksWithMarketAggregates() {
        Task task = publishedTask();
        RewardSummaryResponse rewardSummary = new RewardSummaryResponse(
                "APPROVED_ITEM",
                new BigDecimal("2.50"),
                "POINT"
        );
        when(taskMapper.selectPublishedMarketTasks(eq("qa"), eq("quality"), eq(TaskStatus.PUBLISHED.name()), any(LocalDateTime.class)))
                .thenReturn(List.of(task));
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(taskTag("quality")));
        when(datasetMarketStatsService.countAvailableItems(TASK_ID, LABELER_ID, 2)).thenReturn(3);
        when(assignmentMarketStatsService.countClaimedByLabeler(TASK_ID, LABELER_ID)).thenReturn(1);
        when(rewardSummaryService.findRewardSummary(TASK_ID, true)).thenReturn(rewardSummary);

        List<MarketTaskResponse> responses = taskMarketService.listMarketTasks(
                LABELER_ID,
                new MarketTaskQueryRequest("qa", "quality", TaskStatus.PUBLISHED)
        );

        assertThat(responses).hasSize(1);
        MarketTaskResponse response = responses.get(0);
        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.title()).isEqualTo("QA task");
        assertThat(response.tags()).containsExactly("quality");
        assertThat(response.availableCount()).isEqualTo(3);
        assertThat(response.currentUserClaimedCount()).isEqualTo(1);
        assertThat(response.rewardSummary()).isEqualTo(rewardSummary);
    }

    @Test
    void keepsPublishedTaskWhenAvailableCountIsZero() {
        Task task = publishedTask();
        when(taskMapper.selectPublishedMarketTasks(isNull(), isNull(), isNull(), any(LocalDateTime.class))).thenReturn(List.of(task));
        when(taskTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(datasetMarketStatsService.countAvailableItems(TASK_ID, LABELER_ID, 2)).thenReturn(0);
        when(assignmentMarketStatsService.countClaimedByLabeler(TASK_ID, LABELER_ID)).thenReturn(0);
        when(rewardSummaryService.findRewardSummary(TASK_ID, true)).thenReturn(null);

        List<MarketTaskResponse> responses = taskMarketService.listMarketTasks(
                LABELER_ID,
                new MarketTaskQueryRequest(null, null, null)
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).availableCount()).isZero();
        assertThat(responses.get(0).rewardSummary()).isNull();
    }

    @Test
    void returnsEmptyWhenRequestedStatusIsNotPublished() {
        List<MarketTaskResponse> responses = taskMarketService.listMarketTasks(
                LABELER_ID,
                new MarketTaskQueryRequest(null, null, TaskStatus.PAUSED)
        );

        assertThat(responses).isEmpty();
    }

    private Task publishedTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTitle("QA task");
        task.setStatus(TaskStatus.PUBLISHED);
        task.setOverlapCount(2);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        task.setRewardVisible(true);
        return task;
    }

    private TaskTag taskTag(String tagName) {
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(TASK_ID);
        taskTag.setTagName(tagName);
        return taskTag;
    }
}
