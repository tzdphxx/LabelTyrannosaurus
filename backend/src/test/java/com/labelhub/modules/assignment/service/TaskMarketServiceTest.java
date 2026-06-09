package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskMarketServiceTest {

    private static final Long LABELER_ID = 20L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskTagMapper taskTagMapper;

    @Mock
    private DatasetMarketStatsService datasetMarketStatsService;

    @Mock
    private DatasetItemMapper datasetItemMapper;

    @Mock
    private AssignmentMarketStatsService assignmentMarketStatsService;

    @Mock
    private RewardSummaryService rewardSummaryService;

    @Test
    void listMarketTasksReturnsMaxClaimsPerLabelerOnlyForQuotaGrab() {
        Task quotaGrabTask = task(10L, ClaimStrategy.QUOTA_GRAB, 8);
        Task fcfsTask = task(11L, ClaimStrategy.FCFS, 8);
        TaskMarketService service = new TaskMarketService(
                taskMapper,
                taskTagMapper,
                datasetMarketStatsService,
                datasetItemMapper,
                assignmentMarketStatsService,
                rewardSummaryService
        );

        when(taskMapper.selectPublishedMarketTasks(isNull(), isNull(), isNull(), any(LocalDateTime.class)))
                .thenReturn(List.of(quotaGrabTask, fcfsTask));
        when(taskTagMapper.selectByTaskIds(anyCollection())).thenReturn(List.of());
        when(datasetMarketStatsService.countAvailableItems(anyLong(), eq(LABELER_ID), any())).thenReturn(5);
        when(assignmentMarketStatsService.countClaimedByLabeler(anyLong(), eq(LABELER_ID))).thenReturn(0);
        when(datasetItemMapper.selectClaimableItems(anyLong(), eq(LABELER_ID), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        var responses = service.listMarketTasks(LABELER_ID, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).task().maxClaimsPerLabeler()).isEqualTo(8);
        assertThat(responses.get(1).task().maxClaimsPerLabeler()).isNull();
    }

    private Task task(Long id, ClaimStrategy strategy, Integer maxClaimsPerLabeler) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Task " + id);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setQuota(100);
        task.setClaimedCount(10);
        task.setOverlapCount(1);
        task.setStrategy(strategy);
        task.setMaxClaimsPerLabeler(maxClaimsPerLabeler);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        return task;
    }
}
