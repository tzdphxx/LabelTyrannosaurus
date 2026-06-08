package com.labelhub.modules.assignment.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.mapper.AssignmentDispatchMapper;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.dataset.service.DatasetItemSnapshot;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.service.TemplateSchemaService;
import com.labelhub.modules.template.service.TemplateSchemaSnapshot;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AssignmentClaimService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int TASK_STATUS_NOT_ALLOWED = 400101;
    private static final int CLAIM_CONFLICT = 409201;
    private static final int PERMISSION_DENIED = 403001;
    private static final int QUOTA_EXCEEDED = 409202;
    private static final int CLAIM_LIMIT_EXCEEDED = 409203;
    private static final long CLAIM_LOCK_WAIT_MILLIS = 2000L;
    private static final long CLAIM_LOCK_LEASE_MILLIS = 10000L;
    private static final String ASSIGNMENT_BIZ_TYPE = "ASSIGNMENT";
    private static final String USER_ACTOR_TYPE = "USER";

    private final TaskMapper taskMapper;
    private final DatasetClaimService datasetClaimService;
    private final TemplateSchemaService templateSchemaService;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentDispatchMapper dispatchMapper;
    private final RedisLockService redisLockService;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final TransactionTemplate transactionTemplate;

    public AssignmentClaimService(TaskMapper taskMapper,
                                  DatasetClaimService datasetClaimService,
                                  TemplateSchemaService templateSchemaService,
                                  AssignmentMapper assignmentMapper,
                                  AssignmentDispatchMapper dispatchMapper,
                                  RedisLockService redisLockService,
                                  AuditAppender auditAppender,
                                  TraceIdProvider traceIdProvider,
                                  TransactionTemplate transactionTemplate) {
        this.taskMapper = taskMapper;
        this.datasetClaimService = datasetClaimService;
        this.templateSchemaService = templateSchemaService;
        this.assignmentMapper = assignmentMapper;
        this.dispatchMapper = dispatchMapper;
        this.redisLockService = redisLockService;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.transactionTemplate = transactionTemplate;
    }

    public AssignmentClaimResponse claim(Long taskId, Long labelerId) {
        var currentUser = CurrentUserContext.requireCurrentUser();
        if (!currentUser.hasRole(RoleCode.LABELER)) {
            throw new BusinessException(PERMISSION_DENIED, "当前账号没有权限执行该操作");
        }
        if (!currentUser.isAdmin() && !currentUser.userId().equals(labelerId)) {
            throw new BusinessException(PERMISSION_DENIED, "不能代替其他用户领取任务");
        }
        Task task = loadClaimableTask(taskId);
        ClaimStrategy strategy = task.getStrategy();
        if (strategy == null) {
            strategy = ClaimStrategy.FCFS;
        }
        return switch (strategy) {
            case FCFS -> claimFcfs(task, labelerId);
            case QUOTA_GRAB -> claimQuotaGrab(task, labelerId);
            case ASSIGNED -> claimAssigned(task, labelerId);
        };
    }

    private AssignmentClaimResponse claimFcfs(Task task, Long labelerId) {
        return executeWithLock(task.getId(), () ->
                transactionTemplate.execute(status -> {
                    DatasetItemSnapshot itemSnapshot = datasetClaimService
                            .reserveClaimableItem(task.getId(), labelerId, 1)
                            .orElseThrow(() -> claimConflict("No claimable item is available"));
                    TemplateSchemaSnapshot templateSchema = templateSchemaService
                            .getTemplateSchema(task.getPublishedTemplateVersionId());
                    Assignment assignment = createAssignment(
                            task.getId(), labelerId,
                            itemSnapshot.datasetItemId(),
                            templateSchema.templateVersionId());
                    appendClaimAudit(assignment, itemSnapshot);
                    return buildClaimResponse(assignment, itemSnapshot, templateSchema);
                }));
    }

    private AssignmentClaimResponse claimQuotaGrab(Task task, Long labelerId) {
        return executeWithLock(task.getId(), () ->
                transactionTemplate.execute(status -> {
                    if (taskMapper.tryIncrementClaimedCount(task.getId()) == 0) {
                        throw new BusinessException(QUOTA_EXCEEDED, "Task quota is full");
                    }
                    int maxPerLabeler = task.getMaxClaimsPerLabeler() != null
                            ? task.getMaxClaimsPerLabeler() : Integer.MAX_VALUE;
                    int activeClaims = assignmentMapper.countActiveByTaskAndLabeler(
                            task.getId(), labelerId);
                    if (activeClaims >= maxPerLabeler) {
                        taskMapper.decrementClaimedCount(task.getId());
                        throw new BusinessException(CLAIM_LIMIT_EXCEEDED,
                                "Personal claim limit reached for this task");
                    }
                    DatasetItemSnapshot itemSnapshot = datasetClaimService
                            .reserveClaimableItem(task.getId(), labelerId, 1)
                            .orElseThrow(() -> {
                                taskMapper.decrementClaimedCount(task.getId());
                                return claimConflict("No claimable item is available");
                            });
                    TemplateSchemaSnapshot templateSchema = templateSchemaService
                            .getTemplateSchema(task.getPublishedTemplateVersionId());
                    Assignment assignment = createAssignment(
                            task.getId(), labelerId,
                            itemSnapshot.datasetItemId(),
                            templateSchema.templateVersionId());
                    appendClaimAudit(assignment, itemSnapshot);
                    return buildClaimResponse(assignment, itemSnapshot, templateSchema);
                }));
    }

    private AssignmentClaimResponse claimAssigned(Task task, Long labelerId) {
        return executeWithLock(task.getId(), () ->
                transactionTemplate.execute(status -> {
                    var dispatch = dispatchMapper.selectPendingForLabeler(
                            task.getId(), labelerId);
                    if (dispatch == null) {
                        throw claimConflict("No pending dispatch for this labeler");
                    }
                    if (dispatchMapper.claimById(dispatch.getId()) == 0) {
                        throw claimConflict("Dispatch was already claimed");
                    }
                    DatasetItemSnapshot itemSnapshot = datasetClaimService
                            .reserveSpecificItem(task.getId(), labelerId, dispatch.getDatasetItemId())
                            .orElseThrow(() -> claimConflict("Dataset item could not be reserved"));
                    TemplateSchemaSnapshot templateSchema = templateSchemaService
                            .getTemplateSchema(task.getPublishedTemplateVersionId());
                    Assignment assignment = createAssignment(
                            task.getId(), labelerId,
                            dispatch.getDatasetItemId(),
                            templateSchema.templateVersionId());
                    appendClaimAudit(assignment, itemSnapshot);
                    return buildClaimResponse(assignment, itemSnapshot, templateSchema);
                }));
    }

    private AssignmentClaimResponse buildClaimResponse(Assignment assignment,
                                                       DatasetItemSnapshot itemSnapshot,
                                                       TemplateSchemaSnapshot templateSchema) {
        return new AssignmentClaimResponse(
                assignment.getId(),
                assignment.getDatasetItemId(),
                templateSchema.templateVersionId(),
                templateSchema.schemaJson(),
                itemSnapshot != null ? itemSnapshot.itemJson() : null,
                assignment.getDraftAnswerJson(),
                assignment.getDraftVersion()
        );
    }

    private <T> T executeWithLock(Long taskId, java.util.function.Supplier<T> action) {
        String lockKey = RedisKeyBuilder.taskClaimLock(taskId);
        boolean locked = redisLockService.tryLock(lockKey, CLAIM_LOCK_WAIT_MILLIS, CLAIM_LOCK_LEASE_MILLIS);
        if (!locked) {
            throw claimConflict("Task claim is busy, please retry");
        }
        try {
            return action.get();
        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    private Assignment createAssignment(Long taskId, Long labelerId, Long datasetItemId, Long templateVersionId) {
        Assignment assignment = new Assignment();
        assignment.setTaskId(taskId);
        assignment.setDatasetItemId(datasetItemId);
        assignment.setLabelerId(labelerId);
        assignment.setTemplateVersionId(templateVersionId);
        assignment.setStatus(AssignmentStatus.CLAIMED);
        assignment.setDraftVersion(1);
        try {
            assignmentMapper.insert(assignment);
        } catch (DuplicateKeyException ex) {
            throw claimConflict("Dataset item was already claimed");
        }
        return assignment;
    }

    private Task loadClaimableTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "只有已发布任务可以领取");
        }
        if (task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "任务领取截止时间已过");
        }
        if (task.getPublishedTemplateVersionId() == null) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "任务模板版本缺失");
        }
        if (!Integer.valueOf(1).equals(task.getOverlapCount())) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "任务重叠标注数量必须为 1");
        }
        return task;
    }

    private void appendClaimAudit(Assignment assignment, DatasetItemSnapshot itemSnapshot) {
        Map<String, Object> afterJson = new LinkedHashMap<>();
        afterJson.put("assignmentId", assignment.getId());
        afterJson.put("taskId", assignment.getTaskId());
        afterJson.put("datasetItemId", itemSnapshot.datasetItemId());
        afterJson.put("labelerId", assignment.getLabelerId());
        afterJson.put("status", assignment.getStatus());
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, assignment.getLabelerId(),
                ASSIGNMENT_BIZ_TYPE, assignment.getId(),
                "ASSIGNMENT_CLAIMED", null, afterJson, traceIdProvider.currentTraceId(), null));
    }

    private BusinessException claimConflict(String message) {
        return new BusinessException(CLAIM_CONFLICT, message);
    }
}
