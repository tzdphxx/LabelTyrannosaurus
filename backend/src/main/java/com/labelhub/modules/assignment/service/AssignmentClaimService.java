package com.labelhub.modules.assignment.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
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
    private static final long CLAIM_LOCK_WAIT_MILLIS = 2000L;
    private static final long CLAIM_LOCK_LEASE_MILLIS = 10000L;
    private static final String ASSIGNMENT_BIZ_TYPE = "ASSIGNMENT";
    private static final String USER_ACTOR_TYPE = "USER";

    private final TaskMapper taskMapper;
    private final DatasetClaimService datasetClaimService;
    private final TemplateSchemaService templateSchemaService;
    private final AssignmentMapper assignmentMapper;
    private final RedisLockService redisLockService;
    private final AuditAppender auditAppender;
    private final TransactionTemplate transactionTemplate;

    public AssignmentClaimService(TaskMapper taskMapper,
                                  DatasetClaimService datasetClaimService,
                                  TemplateSchemaService templateSchemaService,
                                  AssignmentMapper assignmentMapper,
                                  RedisLockService redisLockService,
                                  AuditAppender auditAppender,
                                  TransactionTemplate transactionTemplate) {
        this.taskMapper = taskMapper;
        this.datasetClaimService = datasetClaimService;
        this.templateSchemaService = templateSchemaService;
        this.assignmentMapper = assignmentMapper;
        this.redisLockService = redisLockService;
        this.auditAppender = auditAppender;
        this.transactionTemplate = transactionTemplate;
    }

    public AssignmentClaimResponse claim(Long taskId, Long labelerId) {
        if (!CurrentUserContext.requireCurrentUser().roles().contains(RoleCode.LABELER)) {
            throw new BusinessException(PERMISSION_DENIED, "Permission denied");
        }
        Task task = loadClaimableTask(taskId);
        String lockKey = "lock:claim:task:" + taskId;
        boolean locked = redisLockService.tryLock(lockKey, CLAIM_LOCK_WAIT_MILLIS, CLAIM_LOCK_LEASE_MILLIS);
        if (!locked) {
            throw claimConflict("Task claim is busy, please retry");
        }
        try {
            return transactionTemplate.execute(status -> {
                DatasetItemSnapshot itemSnapshot = datasetClaimService
                        .reserveClaimableItem(taskId, labelerId, task.getOverlapCount())
                        .orElseThrow(() -> claimConflict("No claimable item is available"));
                TemplateSchemaSnapshot templateSchema = templateSchemaService.getTemplateSchema(task.getPublishedTemplateVersionId());
                Assignment assignment = createAssignment(taskId, labelerId, itemSnapshot.datasetItemId(), templateSchema.templateVersionId());
                appendClaimAudit(assignment, itemSnapshot);
                return new AssignmentClaimResponse(
                        assignment.getId(),
                        itemSnapshot.datasetItemId(),
                        templateSchema.templateVersionId(),
                        templateSchema.schemaJson(),
                        itemSnapshot.itemJson(),
                        assignment.getDraftAnswerJson(),
                        assignment.getDraftVersion()
                );
            });
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
            throw claimConflict("Dataset item was already claimed by this labeler");
        }
        return assignment;
    }

    private Task loadClaimableTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Only published tasks can be claimed");
        }
        if (task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Task claim deadline has passed");
        }
        if (task.getPublishedTemplateVersionId() == null) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Task template version is missing");
        }
        if (task.getOverlapCount() == null || task.getOverlapCount() < 1) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Task overlap count is invalid");
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
                "ASSIGNMENT_CLAIMED", null, afterJson, null, null));
    }

    private BusinessException claimConflict(String message) {
        return new BusinessException(CLAIM_CONFLICT, message);
    }
}
