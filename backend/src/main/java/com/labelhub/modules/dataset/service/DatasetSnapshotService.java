package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.dto.DatasetItemSnapshot;
import com.labelhub.modules.dataset.repository.DatasetItemRepositoryMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据集快照和领取预留服务。
 *
 * <p>该服务是 BE-B 暴露给 BE-A 的 Java 内部能力。BE-B 只负责返回题目快照、预留数据集计数和维护
 * {@code dataset_items} 统计字段，不创建 assignment，不修改 submission/review 状态。</p>
 */
@Service
public class DatasetSnapshotService {

    private static final int MAX_RESERVE_RETRY = 5;

    private final TaskRepositoryMapper taskMapper;
    private final DatasetItemRepositoryMapper datasetItemMapper;
    private final ObjectMapper objectMapper;

    public DatasetSnapshotService(TaskRepositoryMapper taskMapper,
                                  DatasetItemRepositoryMapper datasetItemMapper,
                                  ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取题目快照，供 BE-A 领取详情、提交和渲染链路使用。
     */
    public DatasetItemSnapshot getDatasetItemSnapshot(Long itemId) {
        DatasetItemEntity item = datasetItemMapper.selectById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            throw new BusinessException(400102, "Dataset item not found");
        }
        return toSnapshot(item);
    }

    /**
     * 预留一个可领取题目。
     *
     * <p>这里仅递增 {@code assigned_count} 作为数据集侧预留标记；BE-A 必须在自己的事务里创建
     * assignment。若未来拆成独立服务，需要补充 reservationId 和确认/释放流程。</p>
     */
    @Transactional
    public DatasetItemSnapshot reserveClaimableItem(Long taskId, Long labelerId) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new BusinessException(400101, "Task is not claimable");
        }
        int overlapLimit = task.getOverlapCount() == null ? 1 : task.getOverlapCount();
        for (int i = 0; i < MAX_RESERVE_RETRY; i++) {
            DatasetItemEntity item = datasetItemMapper.selectClaimableItem(taskId, overlapLimit);
            if (item == null) {
                throw new BusinessException(409201, "No claimable dataset item");
            }
            if (datasetItemMapper.increaseAssignedCount(item.getId(), overlapLimit) > 0) {
                return toSnapshot(item);
            }
        }
        throw new BusinessException(409201, "Dataset item reservation conflict");
    }

    /**
     * 递增提交计数。该入口只能由 BE-A 在提交成功后显式调用。
     */
    public void increaseSubmittedCount(Long itemId) {
        if (datasetItemMapper.increaseSubmittedCount(itemId) == 0) {
            throw new BusinessException(400102, "Dataset item not found");
        }
    }

    /**
     * 递增通过计数。该入口只能由审核通过事件或奖励消费链路显式调用。
     */
    public void increaseApprovedCount(Long itemId) {
        if (datasetItemMapper.increaseApprovedCount(itemId) == 0) {
            throw new BusinessException(400102, "Dataset item not found");
        }
    }

    private DatasetItemSnapshot toSnapshot(DatasetItemEntity item) {
        return new DatasetItemSnapshot(
                item.getId(),
                item.getTaskId(),
                item.getExternalId(),
                item.getDatasetType(),
                readJson(item.getItemJson()),
                readJson(item.getMetadataJson())
        );
    }

    private JsonNode readJson(String json) {
        try {
            return json == null ? objectMapper.nullNode() : objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "Invalid dataset JSON stored");
        }
    }
}
