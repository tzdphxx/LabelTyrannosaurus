package com.labelhub.modules.assignment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.dto.MarketDatasetItemResponse;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
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

    public MarketTaskResponse getMarketTaskDetail(Long labelerId, Long taskId, int itemPage, int itemSize) {
        Task task = taskMapper.selectPublishedMarketTaskById(taskId, LocalDateTime.now());
        if (task == null) {
            throw new BusinessException(MARKET_TASK_NOT_FOUND, "Market task not found");
        }
        return toResponse(labelerId, task, itemPage, itemSize);
    }

    private MarketTaskResponse toResponse(Long labelerId, Task task) {
        return toResponse(labelerId, task, 1, DEFAULT_ITEM_PREVIEW_SIZE);
    }

    private MarketTaskResponse toResponse(Long labelerId, Task task, int itemPage, int itemSize) {
        int normalizedPage = Math.max(1, itemPage);
        int normalizedSize = Math.min(Math.max(1, itemSize), 100);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new MarketTaskResponse(
                task.getId(),
                task.getTitle(),
                listTags(task.getId()),
                task.getDeadlineAt(),
                datasetMarketStatsService.countAvailableItems(task.getId(), labelerId),
                assignmentMarketStatsService.countClaimedByLabeler(task.getId(), labelerId),
                rewardSummaryService.findRewardSummary(task.getId(), Boolean.TRUE.equals(task.getRewardVisible())),
                task.getDescription(),
                task.getInstructionRichText(),
                task.getStatus(),
                task.getQuota(),
                task.getOverlapCount(),
                task.getPublishedTemplateVersionId(),
                listClaimableItems(task, labelerId, normalizedSize, offset)
        );
    }

    private List<MarketDatasetItemResponse> listClaimableItems(Task task,
                                                               Long labelerId,
                                                               int limit,
                                                               int offset) {
        return datasetItemMapper.selectClaimableItems(task.getId(), labelerId, limit, offset)
                .stream()
                .map(this::toMarketDatasetItem)
                .toList();
    }

    private MarketDatasetItemResponse toMarketDatasetItem(DatasetItem item) {
        return new MarketDatasetItemResponse(
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
