package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.dataset.domain.DatasetItemChangeLogEntity;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.dto.BatchDeleteItemsRequest;
import com.labelhub.modules.dataset.dto.BatchItemResult;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetItemPageResponse;
import com.labelhub.modules.dataset.dto.DatasetItemQuery;
import com.labelhub.modules.dataset.dto.DatasetItemResponse;
import com.labelhub.modules.dataset.dto.DatasetItemStatus;
import com.labelhub.modules.dataset.dto.DatasetItemUpdateRequest;
import com.labelhub.modules.dataset.repository.DatasetItemChangeLogMapper;
import com.labelhub.modules.dataset.repository.DatasetItemRepositoryMapper;
import com.labelhub.modules.media.service.MediaProcessingService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dataset item edit service.
 *
 * <p>This service only maintains BE-B-owned {@code dataset_items} and change logs. Items that have been
 * claimed or submitted may already be referenced by BE-A assignment/submission records, so direct mutation
 * is blocked to preserve labeling snapshots.</p>
 */
@Service
public class DatasetItemService {

    private final TaskRepositoryMapper taskMapper;
    private final DatasetItemRepositoryMapper datasetItemMapper;
    private final DatasetItemChangeLogMapper changeLogMapper;
    private final ObjectMapper objectMapper;
    private final MediaProcessingService mediaProcessingService;

    @Autowired
    public DatasetItemService(TaskRepositoryMapper taskMapper,
                              DatasetItemRepositoryMapper datasetItemMapper,
                              DatasetItemChangeLogMapper changeLogMapper,
                              ObjectMapper objectMapper,
                              MediaProcessingService mediaProcessingService) {
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.changeLogMapper = changeLogMapper;
        this.objectMapper = objectMapper;
        this.mediaProcessingService = mediaProcessingService;
    }

    public DatasetItemService(TaskRepositoryMapper taskMapper,
                              DatasetItemRepositoryMapper datasetItemMapper,
                              DatasetItemChangeLogMapper changeLogMapper,
                              ObjectMapper objectMapper) {
        this(taskMapper, datasetItemMapper, changeLogMapper, objectMapper, null);
    }

    /**
     * Lists active dataset items under a task.
     */
    public DatasetItemPageResponse listItems(Long taskId, DatasetItemQuery query) {
        requireOwnedTask(taskId);
        DatasetItemQuery effectiveQuery = query == null ? new DatasetItemQuery(null, null, null) : query;
        List<DatasetItemEntity> entities = datasetItemMapper.selectActivePage(
                taskId,
                effectiveQuery.externalId(),
                effectiveQuery.normalizedPageSize(),
                effectiveQuery.offset()
        );
        long total = datasetItemMapper.countActivePage(taskId, effectiveQuery.externalId());
        return new DatasetItemPageResponse(
                entities.stream().map(this::toResponse).toList(),
                effectiveQuery.normalizedPage(),
                effectiveQuery.normalizedPageSize(),
                total
        );
    }

    /**
     * Batch updates item content. Claimed or submitted items cannot be changed.
     */
    @Transactional
    public List<BatchItemResult> batchUpdate(Long taskId, BatchUpdateItemsRequest request) {
        TaskEntity task = requireOwnedTask(taskId);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        List<BatchItemResult> results = new ArrayList<>();
        for (DatasetItemUpdateRequest itemRequest : request.items()) {
            results.add(updateOne(task, actor.userId(), itemRequest));
        }
        return results;
    }

    /**
     * Batch soft-deletes items. Items already in the labeling flow must be kept for BE-A references.
     */
    @Transactional
    public List<BatchItemResult> batchDelete(Long taskId, BatchDeleteItemsRequest request) {
        TaskEntity task = requireOwnedTask(taskId);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        List<BatchItemResult> results = new ArrayList<>();
        for (Long itemId : request.itemIds()) {
            results.add(deleteOne(task, actor.userId(), itemId));
        }
        return results;
    }


    private BatchItemResult updateOne(TaskEntity task, Long actorId, DatasetItemUpdateRequest request) {
        DatasetItemEntity entity = datasetItemMapper.selectById(request.itemId());
        BatchItemResult validation = validateEditableItem(task, entity, request.itemId());
        if (validation != null) {
            return validation;
        }
        try {
            String beforeJson = entity.getItemJson();
            String itemJson = writeJson(request.itemJson());
            String metadataJson = writeJson(request.metadataJson());
            int updated = datasetItemMapper.updateEditableJsonById(entity.getId(), task.getId(), itemJson, metadataJson);
            if (updated == 0) {
                return BatchItemResult.failure(entity.getId(), entity.getExternalId(), 400101,
                        "Claimed or submitted item cannot be changed");
            }
            refreshMediaContext(task.getId(), entity.getId(), itemJson, actorId);
            appendChangeLog(task.getId(), entity.getId(), "BATCH_UPDATE", beforeJson, itemJson, actorId);
            return BatchItemResult.success(entity.getId(), entity.getExternalId());
        } catch (RuntimeException ex) {
            return BatchItemResult.failure(entity.getId(), entity.getExternalId(), 500001, ex.getMessage());
        }
    }

    private BatchItemResult deleteOne(TaskEntity task, Long actorId, Long itemId) {
        DatasetItemEntity entity = datasetItemMapper.selectById(itemId);
        BatchItemResult validation = validateEditableItem(task, entity, itemId);
        if (validation != null) {
            return validation;
        }
        int deleted = datasetItemMapper.softDeleteById(entity.getId());
        if (deleted == 0) {
            return BatchItemResult.failure(entity.getId(), entity.getExternalId(), 400101,
                    "Claimed or submitted item cannot be changed");
        }
        appendChangeLog(task.getId(), entity.getId(), "BATCH_DELETE", entity.getItemJson(), null, actorId);
        return BatchItemResult.success(entity.getId(), entity.getExternalId());
    }

    private BatchItemResult validateEditableItem(TaskEntity task, DatasetItemEntity entity, Long itemId) {
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted()) || !task.getId().equals(entity.getTaskId())) {
            return BatchItemResult.failure(itemId, null, 400102, "数据项不存在");
        }
        if (positive(entity.getAssignedCount()) || positive(entity.getSubmittedCount())) {
            return BatchItemResult.failure(entity.getId(), entity.getExternalId(), 400101,
                    "Claimed or submitted item cannot be changed");
        }
        return null;
    }

    private TaskEntity requireOwnedTask(Long taskId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "任务不存在");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "当前账号没有权限执行该操作");
        }
        return task;
    }

    private void appendChangeLog(Long taskId,
                                 Long itemId,
                                 String changeType,
                                 String beforeJson,
                                 String afterJson,
                                 Long actorId) {
        DatasetItemChangeLogEntity changeLog = new DatasetItemChangeLogEntity();
        changeLog.setTaskId(taskId);
        changeLog.setItemId(itemId);
        changeLog.setChangeType(changeType);
        changeLog.setBeforeJson(beforeJson);
        changeLog.setAfterJson(afterJson);
        changeLog.setActorId(actorId);
        changeLogMapper.insert(changeLog);
    }

    private void refreshMediaContext(Long taskId, Long itemId, String itemJson, Long actorId) {
        if (mediaProcessingService != null) {
            mediaProcessingService.refreshContext(taskId, itemId, itemJson, actorId);
        }
    }

    private DatasetItemResponse toResponse(DatasetItemEntity entity) {
        return new DatasetItemResponse(
                entity.getId(),
                entity.getTaskId(),
                entity.getExternalId(),
                readJson(entity.getItemJson()),
                readJson(entity.getMetadataJson()),
                entity.getAssignedCount(),
                entity.getSubmittedCount(),
                entity.getApprovedCount(),
                toItemStatus(entity.getAssignmentStatus()),
                entity.getLabelerId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DatasetItemStatus toItemStatus(String assignmentStatus) {
        if (assignmentStatus == null || assignmentStatus.isBlank()) {
            return DatasetItemStatus.UNCLAIMED;
        }
        return switch (assignmentStatus) {
            case "CLAIMED" -> DatasetItemStatus.CLAIMED;
            case "DRAFTING" -> DatasetItemStatus.DRAFT;
            case "SUBMITTED" -> DatasetItemStatus.SUBMITTED;
            case "AI_RETURNED", "RETURNED" -> DatasetItemStatus.RETURNED;
            case "APPROVED" -> DatasetItemStatus.APPROVED;
            case "CANCELLED" -> DatasetItemStatus.UNCLAIMED;
            default -> throw new BusinessException(500001, "数据项的领取状态未知");
        };
    }

    private String writeJson(Map<String, Object> json) {
        try {
            return objectMapper.writeValueAsString(json == null ? Map.of() : json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400102, "JSON 请求内容不合法");
        }
    }

    private JsonNode readJson(String json) {
        try {
            return json == null ? objectMapper.nullNode() : objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "已存储的数据集 JSON 不合法");
        }
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
