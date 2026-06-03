package com.labelhub.modules.assignment.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.AssignmentDispatch;
import com.labelhub.modules.assignment.dto.DispatchEntryResponse;
import com.labelhub.modules.assignment.dto.DispatchRequest;
import com.labelhub.modules.assignment.mapper.AssignmentDispatchMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentDispatchService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int PERMISSION_DENIED = 403001;
    private static final int BAD_REQUEST = 400001;
    private static final int CONFLICT = 409001;

    private final TaskMapper taskMapper;
    private final AssignmentDispatchMapper dispatchMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final UserRoleMapper userRoleMapper;

    public AssignmentDispatchService(TaskMapper taskMapper,
                                     AssignmentDispatchMapper dispatchMapper,
                                     DatasetItemMapper datasetItemMapper,
                                     UserRoleMapper userRoleMapper) {
        this.taskMapper = taskMapper;
        this.dispatchMapper = dispatchMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional
    public List<DispatchEntryResponse> dispatch(Long taskId, Long ownerId,
                                                 DispatchRequest request) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getOwnerId().equals(ownerId)) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        if (task.getStrategy() != ClaimStrategy.ASSIGNED) {
            throw new BusinessException(BAD_REQUEST,
                    "Dispatch is only available for ASSIGNED strategy");
        }
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(BAD_REQUEST,
                    "Dispatches can only be created while the task is in DRAFT status");
        }

        // Batch pre-fetch dataset items and labeler roles
        Set<Long> itemIds = request.dispatches().stream()
                .map(DispatchRequest.DispatchEntry::datasetItemId)
                .collect(Collectors.toSet());
        Set<Long> labelerIds = request.dispatches().stream()
                .map(DispatchRequest.DispatchEntry::labelerId)
                .collect(Collectors.toSet());

        Map<Long, DatasetItem> itemMap = datasetItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(DatasetItem::getId, Function.identity()));

        Map<Long, Set<RoleCode>> roleMap = new HashMap<>();
        for (Long labelerId : labelerIds) {
            roleMap.put(labelerId, userRoleMapper.selectRoleCodesByUserId(labelerId));
        }

        List<DispatchEntryResponse> results = new ArrayList<>();
        for (var entry : request.dispatches()) {
            int existing = dispatchMapper.countPendingByItem(taskId, entry.datasetItemId());
            if (existing > 0) {
                throw new BusinessException(CONFLICT,
                        "Dataset item " + entry.datasetItemId()
                                + " already has a pending dispatch");
            }

            DatasetItem item = itemMap.get(entry.datasetItemId());
            if (item == null || !item.getTaskId().equals(taskId)) {
                throw new BusinessException(BAD_REQUEST,
                        "Dataset item " + entry.datasetItemId()
                                + " does not belong to this task");
            }

            Set<RoleCode> roles = roleMap.get(entry.labelerId());
            if (roles == null || !roles.contains(RoleCode.LABELER)) {
                throw new BusinessException(BAD_REQUEST,
                        "User " + entry.labelerId() + " is not a labeler");
            }

            AssignmentDispatch dispatch = new AssignmentDispatch();
            dispatch.setTaskId(taskId);
            dispatch.setDatasetItemId(entry.datasetItemId());
            dispatch.setLabelerId(entry.labelerId());
            dispatch.setStatus("PENDING");
            dispatch.setDispatchedAt(LocalDateTime.now());
            dispatchMapper.insert(dispatch);

            results.add(new DispatchEntryResponse(
                    dispatch.getId(),
                    taskId,
                    entry.datasetItemId(),
                    entry.labelerId(),
                    "PENDING",
                    dispatch.getDispatchedAt(),
                    null));
        }
        syncQuota(taskId);
        return results;
    }

    @Transactional
    public void revokeDispatch(Long taskId, Long ownerId, Long dispatchId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getOwnerId().equals(ownerId)) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        int updated = dispatchMapper.revokeById(dispatchId);
        if (updated == 0) {
            throw new BusinessException(CONFLICT,
                    "Dispatch not found or already claimed/revoked");
        }
        syncQuota(taskId);
    }

    public List<DispatchEntryResponse> listDispatches(Long taskId, Long ownerId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getOwnerId().equals(ownerId)) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        return dispatchMapper.selectByTask(taskId).stream()
                .map(d -> new DispatchEntryResponse(
                        d.getId(), d.getTaskId(), d.getDatasetItemId(),
                        d.getLabelerId(), d.getStatus(),
                        d.getDispatchedAt(), d.getClaimedAt()))
                .toList();
    }

    private void syncQuota(Long taskId) {
        dispatchMapper.syncQuotaToTask(taskId);
    }

    public List<DispatchEntryResponse> listMyDispatches(Long taskId, Long labelerId) {
        return dispatchMapper.selectForLabeler(taskId, labelerId).stream()
                .map(d -> new DispatchEntryResponse(
                        d.getId(), d.getTaskId(), d.getDatasetItemId(),
                        d.getLabelerId(), d.getStatus(),
                        d.getDispatchedAt(), d.getClaimedAt()))
                .toList();
    }
}
