package com.labelhub.modules.assignment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.dto.LabelerTaskDetailResponse;
import com.labelhub.modules.assignment.dto.LabelerTaskTemplateResponse;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.dto.ItemSummaryResponse;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LabelerTaskWorkspaceService {

    private static final int TASK_NOT_FOUND = 404501;
    private static final int TEMPLATE_NOT_FOUND = 404502;
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final DatasetMarketStatsService datasetMarketStatsService;
    private final AssignmentMarketStatsService assignmentMarketStatsService;
    private final RewardSummaryService rewardSummaryService;
    private final TemplateVersionMapper templateVersionMapper;

    public LabelerTaskWorkspaceService(TaskMapper taskMapper,
                                       TaskTagMapper taskTagMapper,
                                       DatasetItemMapper datasetItemMapper,
                                       DatasetMarketStatsService datasetMarketStatsService,
                                       AssignmentMarketStatsService assignmentMarketStatsService,
                                       RewardSummaryService rewardSummaryService,
                                       TemplateVersionMapper templateVersionMapper) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.datasetMarketStatsService = datasetMarketStatsService;
        this.assignmentMarketStatsService = assignmentMarketStatsService;
        this.rewardSummaryService = rewardSummaryService;
        this.templateVersionMapper = templateVersionMapper;
    }

    public LabelerTaskDetailResponse getTaskDetail(Long labelerId, Long taskId, int itemPage, int itemSize) {
        Task task = requireVisibleTask(taskId);
        int page = Math.max(1, itemPage);
        int size = Math.max(1, Math.min(itemSize, MAX_PAGE_SIZE));
        int offset = (page - 1) * size;
        return new LabelerTaskDetailResponse(
                toSummary(task),
                task.getDescription(),
                task.getInstructionRichText(),
                task.getPublishedTemplateVersionId(),
                datasetMarketStatsService.countAvailableItems(taskId, labelerId, task.getOverlapCount()),
                assignmentMarketStatsService.countClaimedByLabeler(taskId, labelerId),
                rewardSummaryService.findRewardSummary(taskId, Boolean.TRUE.equals(task.getRewardVisible())),
                datasetItemMapper.selectClaimableItems(taskId, labelerId, task.getOverlapCount(), size, offset)
                        .stream()
                        .map(this::toItemSummary)
                        .toList()
        );
    }

    public LabelerTaskTemplateResponse getAnswerTemplate(Long labelerId, Long taskId) {
        Task task = requireVisibleTask(taskId);
        Long versionId = task.getPublishedTemplateVersionId();
        if (versionId == null) {
            throw new BusinessException(TEMPLATE_NOT_FOUND, "Task answer template not found");
        }
        TemplateVersion version = templateVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(TEMPLATE_NOT_FOUND, "Task answer template not found");
        }
        return new LabelerTaskTemplateResponse(taskId, versionId, version.getSchemaJson());
    }

    private Task requireVisibleTask(Long taskId) {
        Task task = taskMapper.selectPublishedMarketTaskById(taskId, LocalDateTime.now());
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Market task not found");
        }
        return task;
    }

    private TaskSummaryResponse toSummary(Task task) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                listTags(task.getId()),
                task.getQuota(),
                task.getClaimedCount(),
                task.getOverlapCount(),
                task.getStrategy(),
                maxClaimsPerLabeler(task),
                task.getDeadlineAt(),
                task.getPublishedAt(),
                task.getEndedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                null
        );
    }

    private Integer maxClaimsPerLabeler(Task task) {
        return task.getStrategy() == ClaimStrategy.QUOTA_GRAB ? task.getMaxClaimsPerLabeler() : null;
    }

    private List<String> listTags(Long taskId) {
        return taskTagMapper.selectList(new QueryWrapper<TaskTag>()
                        .eq("task_id", taskId)
                        .orderByAsc("id"))
                .stream()
                .map(TaskTag::getTagName)
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
}
