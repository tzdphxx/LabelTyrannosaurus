package com.labelhub.modules.assignment.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.dataset.service.DatasetItemSnapshot;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.template.service.TemplateSchemaService;
import com.labelhub.modules.template.service.TemplateSchemaSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AssignedAutoAssignmentService {

    private static final int CLAIM_CONFLICT = 409201;
    private static final String ASSIGNMENT_BIZ_TYPE = "ASSIGNMENT";
    private static final String USER_ACTOR_TYPE = "USER";

    private final DatasetClaimService datasetClaimService;
    private final TemplateSchemaService templateSchemaService;
    private final AssignmentMapper assignmentMapper;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;

    public AssignedAutoAssignmentService(DatasetClaimService datasetClaimService,
                                         TemplateSchemaService templateSchemaService,
                                         AssignmentMapper assignmentMapper,
                                         AuditAppender auditAppender,
                                         TraceIdProvider traceIdProvider) {
        this.datasetClaimService = datasetClaimService;
        this.templateSchemaService = templateSchemaService;
        this.assignmentMapper = assignmentMapper;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
    }

    public int autoClaimAll(Task task, Long labelerId) {
        TemplateSchemaSnapshot templateSchema = templateSchemaService
                .getTemplateSchema(task.getPublishedTemplateVersionId());
        int claimedCount = 0;
        while (true) {
            var itemSnapshot = datasetClaimService
                    .reserveClaimableItem(task.getId(), labelerId, task.getOverlapCount());
            if (itemSnapshot.isEmpty()) {
                return claimedCount;
            }
            Assignment assignment = createAssignment(
                    task.getId(),
                    labelerId,
                    itemSnapshot.get().datasetItemId(),
                    templateSchema.templateVersionId());
            appendClaimAudit(assignment, itemSnapshot.get());
            claimedCount++;
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
            throw new BusinessException(CLAIM_CONFLICT, "Dataset item was already claimed");
        }
        return assignment;
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
}
