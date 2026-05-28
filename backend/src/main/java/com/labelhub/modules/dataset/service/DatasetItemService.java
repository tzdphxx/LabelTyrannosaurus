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
import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchDeleteItemsRequest;
import com.labelhub.modules.dataset.dto.BatchItemResult;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetItemAppendRequest;
import com.labelhub.modules.dataset.dto.DatasetItemPageResponse;
import com.labelhub.modules.dataset.dto.DatasetItemQuery;
import com.labelhub.modules.dataset.dto.DatasetItemResponse;
import com.labelhub.modules.dataset.dto.DatasetItemUpdateRequest;
import com.labelhub.modules.dataset.repository.DatasetItemChangeLogMapper;
import com.labelhub.modules.dataset.repository.DatasetItemMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 题目数据资产编辑服务。
 *
 * <p>本服务只维护 BE-B 拥有的 {@code dataset_items} 和变更日志。已领取或已提交的题目
 * 可能已被 BE-A 的 assignment/submission 引用，因此禁止直接修改或删除，避免破坏标注链路快照。</p>
 */
@Service
public class DatasetItemService {

    private final TaskMapper taskMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final DatasetItemChangeLogMapper changeLogMapper;
    private final ObjectMapper objectMapper;

    public DatasetItemService(TaskMapper taskMapper,
                              DatasetItemMapper datasetItemMapper,
                              DatasetItemChangeLogMapper changeLogMapper,
                              ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.changeLogMapper = changeLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询任务下未删除题目列表。
     */
    public DatasetItemPageResponse listItems(Long taskId, DatasetItemQuery query) {
        requireOwnedTask(taskId);
        DatasetItemQuery effectiveQuery = query == null ? new DatasetItemQuery(null, null, null, null) : query;
        String datasetType = effectiveQuery.datasetType() == null ? null : effectiveQuery.datasetType().name();
        List<DatasetItemEntity> entities = datasetItemMapper.selectActivePage(
                taskId,
                datasetType,
                effectiveQuery.externalId(),
                effectiveQuery.normalizedPageSize(),
                effectiveQuery.offset()
        );
        long total = datasetItemMapper.countActivePage(taskId, datasetType, effectiveQuery.externalId());
        return new DatasetItemPageResponse(
                entities.stream().map(this::toResponse).toList(),
                effectiveQuery.normalizedPage(),
                effectiveQuery.normalizedPageSize(),
                total
        );
    }

    /**
     * 批量追加题目。单条失败不会回滚其他成功题目。
     */
    @Transactional
    public List<BatchItemResult> batchAppend(Long taskId, BatchAppendItemsRequest request) {
        TaskEntity task = requireOwnedTask(taskId);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        List<BatchItemResult> results = new ArrayList<>();
        Set<String> seenExternalIds = new HashSet<>();
        for (DatasetItemAppendRequest itemRequest : request.items()) {
            results.add(appendOne(task, actor.userId(), itemRequest, seenExternalIds));
        }
        return results;
    }

    /**
     * 批量更新题目内容。已领取或已提交题目不允许修改。
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
     * 批量软删除题目。已进入标注链路的题目必须保留，避免 BE-B 破坏 BE-A 引用。
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

    private BatchItemResult appendOne(TaskEntity task,
                                      Long actorId,
                                      DatasetItemAppendRequest request,
                                      Set<String> seenExternalIds) {
        String externalId = request.externalId();
        try {
            if (!seenExternalIds.add(externalId)
                    || datasetItemMapper.selectActiveByTaskIdAndExternalId(task.getId(), externalId) != null) {
                return BatchItemResult.failure(null, externalId, 400102, "externalId already exists in this task");
            }
            DatasetItemEntity entity = new DatasetItemEntity();
            entity.setTaskId(task.getId());
            entity.setExternalId(externalId);
            entity.setDatasetType(request.datasetType().name());
            entity.setItemJson(writeJson(request.itemJson()));
            entity.setMetadataJson(writeJson(request.metadataJson()));
            entity.setAssignedCount(0);
            entity.setSubmittedCount(0);
            entity.setApprovedCount(0);
            entity.setDeleted(false);
            datasetItemMapper.insert(entity);
            appendChangeLog(task.getId(), entity.getId(), "BATCH_APPEND", null, entity.getItemJson(), actorId);
            return BatchItemResult.success(entity.getId(), externalId);
        } catch (RuntimeException ex) {
            return BatchItemResult.failure(null, externalId, 500001, ex.getMessage());
        }
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
            return BatchItemResult.failure(itemId, null, 400102, "Dataset item not found");
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
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
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

    private DatasetItemResponse toResponse(DatasetItemEntity entity) {
        return new DatasetItemResponse(
                entity.getId(),
                entity.getTaskId(),
                entity.getExternalId(),
                entity.getDatasetType(),
                readJson(entity.getItemJson()),
                readJson(entity.getMetadataJson()),
                entity.getAssignedCount(),
                entity.getSubmittedCount(),
                entity.getApprovedCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeJson(Map<String, Object> json) {
        try {
            return objectMapper.writeValueAsString(json == null ? Map.of() : json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400102, "Invalid JSON payload");
        }
    }

    private JsonNode readJson(String json) {
        try {
            return json == null ? objectMapper.nullNode() : objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "Invalid dataset JSON stored");
        }
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
