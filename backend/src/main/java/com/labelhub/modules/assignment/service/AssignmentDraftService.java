package com.labelhub.modules.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentDraftService {

    private static final int ASSIGNMENT_NOT_FOUND = 404301;
    private static final int ASSIGNMENT_STATUS_NOT_EDITABLE = 400301;
    private static final int INVALID_ANSWER_JSON = 400302;
    private static final int DRAFT_VERSION_CONFLICT = 409101;
    private static final String ASSIGNMENT_BIZ_TYPE = "ASSIGNMENT";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final Set<AssignmentStatus> EDITABLE_STATUSES = Set.of(
            AssignmentStatus.CLAIMED,
            AssignmentStatus.DRAFTING,
            AssignmentStatus.RETURNED
    );

    private final AssignmentMapper assignmentMapper;
    private final AssignmentDraftCacheService assignmentDraftCacheService;
    private final AuditAppender auditAppender;
    private final ObjectMapper objectMapper;

    public AssignmentDraftService(AssignmentMapper assignmentMapper,
                                  AssignmentDraftCacheService assignmentDraftCacheService,
                                  AuditAppender auditAppender,
                                  ObjectMapper objectMapper) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentDraftCacheService = assignmentDraftCacheService;
        this.auditAppender = auditAppender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssignmentDraftResponse saveDraft(Long assignmentId,
                                             Long labelerId,
                                             AssignmentDraftSaveRequest request) {
        Assignment assignment = loadOwnedAssignment(assignmentId, labelerId);
        requireEditable(assignment);
        requireValidJson(request.answerJson());
        requireCurrentVersion(assignment, request.clientVersion());
        int nextDraftVersion = assignment.getDraftVersion() + 1;
        int updated = assignmentMapper.updateDraftIfVersionMatches(
                assignmentId,
                labelerId,
                request.answerJson(),
                request.clientVersion(),
                nextDraftVersion,
                AssignmentStatus.DRAFTING
        );
        if (updated != 1) {
            throw new BusinessException(DRAFT_VERSION_CONFLICT, "Draft version conflict");
        }
        LocalDateTime updatedAt = LocalDateTime.now();
        AssignmentDraftCacheEntry cacheEntry = new AssignmentDraftCacheEntry(
                assignmentId,
                labelerId,
                request.answerJson(),
                nextDraftVersion,
                AssignmentStatus.DRAFTING,
                updatedAt
        );
        assignmentDraftCacheService.put(cacheEntry);
        appendDraftAudit(assignment, cacheEntry);
        return toResponse(cacheEntry);
    }

    public AssignmentDraftResponse getDraft(Long assignmentId, Long labelerId) {
        return assignmentDraftCacheService.get(assignmentId)
                .filter(entry -> Objects.equals(entry.labelerId(), labelerId))
                .map(this::toResponse)
                .orElseGet(() -> loadDraftFromMysql(assignmentId, labelerId));
    }

    private AssignmentDraftResponse loadDraftFromMysql(Long assignmentId, Long labelerId) {
        Assignment assignment = loadOwnedAssignment(assignmentId, labelerId);
        AssignmentDraftCacheEntry cacheEntry = new AssignmentDraftCacheEntry(
                assignment.getId(),
                assignment.getLabelerId(),
                assignment.getDraftAnswerJson(),
                assignment.getDraftVersion(),
                assignment.getStatus(),
                assignment.getUpdatedAt()
        );
        assignmentDraftCacheService.put(cacheEntry);
        return toResponse(cacheEntry);
    }

    private Assignment loadOwnedAssignment(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, labelerId);
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    private void requireEditable(Assignment assignment) {
        if (!EDITABLE_STATUSES.contains(assignment.getStatus())) {
            throw new BusinessException(ASSIGNMENT_STATUS_NOT_EDITABLE, "Assignment status is not editable");
        }
    }

    private void requireValidJson(String answerJson) {
        try {
            objectMapper.readTree(answerJson);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(INVALID_ANSWER_JSON, "Answer JSON is invalid");
        }
    }

    private void requireCurrentVersion(Assignment assignment, Integer clientDraftVersion) {
        if (!Objects.equals(assignment.getDraftVersion(), clientDraftVersion)) {
            throw new BusinessException(DRAFT_VERSION_CONFLICT, "Draft version conflict");
        }
    }

    private void appendDraftAudit(Assignment beforeAssignment, AssignmentDraftCacheEntry afterEntry) {
        Map<String, Object> beforeJson = new LinkedHashMap<>();
        beforeJson.put("assignmentId", beforeAssignment.getId());
        beforeJson.put("status", beforeAssignment.getStatus());
        beforeJson.put("draftVersion", beforeAssignment.getDraftVersion());

        Map<String, Object> afterJson = new LinkedHashMap<>();
        afterJson.put("assignmentId", afterEntry.assignmentId());
        afterJson.put("status", afterEntry.status());
        afterJson.put("draftVersion", afterEntry.draftVersion());
        afterJson.put("answerLength", afterEntry.draftAnswerJson() == null ? 0 : afterEntry.draftAnswerJson().length());

        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, afterEntry.labelerId(),
                ASSIGNMENT_BIZ_TYPE, afterEntry.assignmentId(),
                "ASSIGNMENT_DRAFT_SAVED", beforeJson, afterJson, null, null));
    }

    private AssignmentDraftResponse toResponse(AssignmentDraftCacheEntry entry) {
        return new AssignmentDraftResponse(
                entry.assignmentId(),
                entry.draftAnswerJson(),
                entry.draftVersion(),
                entry.status(),
                entry.updatedAt()
        );
    }
}
