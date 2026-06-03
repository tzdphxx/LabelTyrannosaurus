package com.labelhub.modules.assignment.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AssignmentCancelService {

    private static final int ASSIGNMENT_NOT_FOUND = 404401;
    private static final int FORBIDDEN = 403401;
    private static final int CANCEL_CONFLICT = 409401;

    private final AssignmentMapper assignmentMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TaskMapper taskMapper;
    private final TransactionTemplate transactionTemplate;
    private final AuditAppender auditAppender;

    public AssignmentCancelService(AssignmentMapper assignmentMapper,
                                   DatasetItemMapper datasetItemMapper,
                                   TaskMapper taskMapper,
                                   TransactionTemplate transactionTemplate,
                                   AuditAppender auditAppender) {
        this.assignmentMapper = assignmentMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.taskMapper = taskMapper;
        this.transactionTemplate = transactionTemplate;
        this.auditAppender = auditAppender;
    }

    public void cancel(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "Assignment not found");
        }
        if (!labelerId.equals(assignment.getLabelerId())) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }

        transactionTemplate.executeWithoutResult(tx -> {
            int updated = assignmentMapper.markCancelled(assignmentId, labelerId);
            if (updated == 0) {
                throw new BusinessException(CANCEL_CONFLICT,
                        "Assignment cannot be cancelled in current status");
            }
            datasetItemMapper.decreaseAssignedCount(assignment.getDatasetItemId());

            Task task = taskMapper.selectById(assignment.getTaskId());
            if (task != null && task.getStrategy() == ClaimStrategy.QUOTA_GRAB) {
                taskMapper.decrementClaimedCount(task.getId());
            }
        });

        auditAppender.append(new AuditCommand(
                "USER", labelerId, "ASSIGNMENT", assignmentId,
                "ASSIGNMENT_CANCELLED",
                Map.of("status", assignment.getStatus().name()),
                Map.of("status", "CANCELLED"),
                null, null));
    }
}