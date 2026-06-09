package com.labelhub.modules.assignment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.TaskMarketResponse;
import com.labelhub.modules.dataset.dto.ItemSummaryResponse;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskMarketService {

    private static final int MARKET_TASK_NOT_FOUND = 404501;
    private static final int DEFAULT_ITEM_PREVIEW_SIZE = 20;

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final DatasetMarketStatsService datasetMarketStatsService;
    private final DatasetItemMapper datasetItemMapper;
    private final AssignmentMarketStatsService assignmentMarketStatsService;
    private final RewardSummaryService rewardSummaryService;

    public TaskMarketService(TaskMapper taskMapper,
                             TaskTagMapper taskTagMapper,
                             DatasetMarketStatsService datasetMarketStatsService,
                             DatasetItemMapper datasetItemMapper,
                             AssignmentMarketStatsService assignmentMarketStatsService,
                             RewardSummaryService rewardSummaryService) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.datasetMarketStatsService = datasetMarketStatsService;
        this.datasetItemMapper = datasetItemMapper;
        this.assignmentMarketStatsService = assignmentMarketStatsService;
        this.rewardSummaryService = rewardSummaryService;
    }

    public List<TaskMarketResponse> listMarketTasks(Long labelerId, MarketTaskQueryRequest request) {
        if (request != null && request.status() != null && request.status() != TaskStatus.PUBLISHED) {
            return List.of();
        }
        String keyword = normalize(request == null ? null : request.keyword());
        String tag = normalize(request == null ? null : request.tag());
        String status = request == null || request.status() == null ? null : request.status().name();
        List<Task> tasks = taskMapper.selectPublishedMarketTasks(keyword, tag, status, LocalDateTime.now());
        Map<Long, List<String>> tagsByTask = loadTags(tasks.stream().map(Task::getId).toList());
        return tasks.stream()
                .map(task -> toResponse(labelerId, task, 1, DEFAULT_ITEM_PREVIEW_SIZE,
                        tagsByTask.getOrDefault(task.getId(), List.of())))
                .toList();
    }

    public TaskMarketResponse getMarketTaskDetail(Long labelerId, Long taskId, int itemPage, int itemSize) {
        Task task = taskMapper.selectPublishedMarketTaskById(taskId, LocalDateTime.now());
        if (task == null) {
            throw new BusinessException(MARKET_TASK_NOT_FOUND, "任务广场中的任务不存在");
        }
        return toResponse(labelerId, task, itemPage, itemSize, listTags(taskId));
    }

    private TaskMarketResponse toResponse(Long labelerId, Task task, int itemPage, int itemSize,
                                          List<String> tags) {
        int normalizedPage = Math.max(1, itemPage);
        int normalizedSize = Math.min(Math.max(1, itemSize), 100);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new TaskMarketResponse(
                toSummary(task, tags),
                datasetMarketStatsService.countAvailableItems(task.getId(), labelerId, task.getOverlapCount()),
                assignmentMarketStatsService.countClaimedByLabeler(task.getId(), labelerId),
                rewardSummaryService.findRewardSummary(task.getId(), Boolean.TRUE.equals(task.getRewardVisible())),
                task.getDescription(),
                task.getInstructionRichText(),
                listClaimableItems(task, labelerId, normalizedSize, offset)
        );
    }

    private TaskSummaryResponse toSummary(Task task, List<String> tags) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                tags,
                task.getQuota(),
                task.getClaimedCount(),
                task.getOverlapCount(),
                task.getStrategy(),
                maxClaimsPerLabeler(task),
                task.getDeadlineAt(),
                task.getPublishedAt(),
                task.getEndedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private Integer maxClaimsPerLabeler(Task task) {
        return task.getStrategy() == ClaimStrategy.QUOTA_GRAB ? task.getMaxClaimsPerLabeler() : null;
    }

    private List<ItemSummaryResponse> listClaimableItems(Task task,
                                                          Long labelerId,
                                                          int limit,
                                                          int offset) {
        return datasetItemMapper.selectClaimableItems(task.getId(), labelerId, task.getOverlapCount(), limit, offset)
                .stream()
                .map(this::toItemSummary)
                .toList();
    }

    private ItemSummaryResponse toItemSummary(DatasetItem item) {
        return new ItemSummaryResponse(
                item.getId(),
                item.getExternalId(),
                item.getItemJson(),
                item.getMetadataJson()
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

    private Map<Long, List<String>> loadTags(List<Long> taskIds) {
        List<Long> ids = taskIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return taskTagMapper.selectByTaskIds(ids).stream()
                .collect(Collectors.groupingBy(
                        TaskTag::getTaskId,
                        Collectors.mapping(TaskTag::getTagName, Collectors.toList())));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
