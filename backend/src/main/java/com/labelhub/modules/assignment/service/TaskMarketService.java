package com.labelhub.modules.assignment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskMarketService {

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final DatasetMarketStatsService datasetMarketStatsService;
    private final AssignmentMarketStatsService assignmentMarketStatsService;
    private final RewardSummaryService rewardSummaryService;

    public TaskMarketService(TaskMapper taskMapper,
                             TaskTagMapper taskTagMapper,
                             DatasetMarketStatsService datasetMarketStatsService,
                             AssignmentMarketStatsService assignmentMarketStatsService,
                             RewardSummaryService rewardSummaryService) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.datasetMarketStatsService = datasetMarketStatsService;
        this.assignmentMarketStatsService = assignmentMarketStatsService;
        this.rewardSummaryService = rewardSummaryService;
    }

    public List<MarketTaskResponse> listMarketTasks(Long labelerId, MarketTaskQueryRequest request) {
        if (request != null && request.status() != null && request.status() != TaskStatus.PUBLISHED) {
            return List.of();
        }
        String keyword = normalize(request == null ? null : request.keyword());
        String tag = normalize(request == null ? null : request.tag());
        String status = request == null || request.status() == null ? null : request.status().name();
        return taskMapper.selectPublishedMarketTasks(keyword, tag, status, LocalDateTime.now())
                .stream()
                .map(task -> toResponse(labelerId, task))
                .toList();
    }

    private MarketTaskResponse toResponse(Long labelerId, Task task) {
        return new MarketTaskResponse(
                task.getId(),
                task.getTitle(),
                listTags(task.getId()),
                task.getDeadlineAt(),
                datasetMarketStatsService.countAvailableItems(task.getId(), labelerId, task.getOverlapCount()),
                assignmentMarketStatsService.countClaimedByLabeler(task.getId(), labelerId),
                rewardSummaryService.findRewardSummary(task.getId(), Boolean.TRUE.equals(task.getRewardVisible()))
        );
    }

    private List<String> listTags(Long taskId) {
        return taskTagMapper.selectList(new QueryWrapper<TaskTag>()
                        .eq("task_id", taskId)
                        .orderByAsc("id"))
                .stream()
                .map(TaskTag::getTagName)
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
