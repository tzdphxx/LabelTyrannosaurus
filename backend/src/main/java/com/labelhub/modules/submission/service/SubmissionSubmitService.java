package com.labelhub.modules.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.service.AiReviewDispatcher;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.SubmissionSubmitRequest;
import com.labelhub.modules.submission.dto.SubmissionSubmitResponse;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.service.AnswerSchemaValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionSubmitService {

    private static final int ASSIGNMENT_NOT_FOUND = 404401;
    private static final int ASSIGNMENT_STATUS_NOT_SUBMITTABLE = 400401;
    private static final int INVALID_ANSWER_JSON = 400402;
    private static final int TASK_NOT_SUBMITTABLE = 400403;
    private static final int DRAFT_VERSION_CONFLICT = 409101;
    private static final String ASSIGNMENT_BIZ_TYPE = "ASSIGNMENT";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final String AI_REVIEW_AGENT_TYPE = "AI_REVIEW";
    private static final Set<AssignmentStatus> SUBMITTABLE_STATUSES = Set.of(
            AssignmentStatus.CLAIMED,
            AssignmentStatus.DRAFTING,
            AssignmentStatus.RETURNED
    );

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final AgentRunMapper agentRunMapper;
    private final AnswerSchemaValidator answerSchemaValidator;
    private final AuditAppender auditAppender;
    private final AiReviewDispatcher aiReviewDispatcher;
    private final DatasetClaimService datasetClaimService;
    private final ObjectMapper objectMapper;

    @Autowired
    public SubmissionSubmitService(AssignmentMapper assignmentMapper,
                                   SubmissionMapper submissionMapper,
                                   TaskMapper taskMapper,
                                   AgentRunMapper agentRunMapper,
                                   AnswerSchemaValidator answerSchemaValidator,
                                   AuditAppender auditAppender,
                                   AiReviewDispatcher aiReviewDispatcher,
                                   DatasetClaimService datasetClaimService) {
        this(assignmentMapper, submissionMapper, taskMapper, agentRunMapper, answerSchemaValidator, auditAppender,
                aiReviewDispatcher, datasetClaimService, new ObjectMapper());
    }

    SubmissionSubmitService(AssignmentMapper assignmentMapper,
                            SubmissionMapper submissionMapper,
                            TaskMapper taskMapper,
                            AgentRunMapper agentRunMapper,
                            AnswerSchemaValidator answerSchemaValidator,
                            AuditAppender auditAppender,
                            AiReviewDispatcher aiReviewDispatcher,
                            DatasetClaimService datasetClaimService,
                            ObjectMapper objectMapper) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.agentRunMapper = agentRunMapper;
        this.answerSchemaValidator = answerSchemaValidator;
        this.auditAppender = auditAppender;
        this.aiReviewDispatcher = aiReviewDispatcher;
        this.datasetClaimService = datasetClaimService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmissionSubmitResponse submit(Long assignmentId,
                                           Long labelerId,
                                           SubmissionSubmitRequest request) {
        Assignment assignment = loadOwnedAssignment(assignmentId, labelerId);
        requireCurrentDraftVersion(assignment, request.clientVersion());
        String canonicalAnswerJson = canonicalAnswerJson(request.answerJson());
        answerSchemaValidator.validateAnswer(assignment.getTemplateVersionId(), canonicalAnswerJson);
        Task task = loadSubmittableTask(assignment.getTaskId());
        String answerHash = sha256(canonicalAnswerJson);
        Submission latestActive = submissionMapper.selectLatestActiveByAssignmentId(assignmentId);
        if (latestActive != null && Objects.equals(latestActive.getAnswerHash(), answerHash)) {
            return toResponse(latestActive, null);
        }
        requireSubmittableStatus(assignment);
        Submission latest = submissionMapper.selectLatestByAssignmentId(assignmentId);
        int nextVersionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        if (latestActive != null) {
            submissionMapper.supersedeActiveByAssignmentId(assignmentId);
        }
        Submission submission = createSubmission(assignment, canonicalAnswerJson, answerHash, nextVersionNo);
        int updated = assignmentMapper.markSubmittedIfCurrent(
                assignmentId,
                labelerId,
                request.clientVersion(),
                AssignmentStatus.SUBMITTED
        );
        if (updated != 1) {
            throw new BusinessException(DRAFT_VERSION_CONFLICT, "Draft version conflict");
        }
        AgentRun agentRun = createPendingAiReviewRun(submission, task);
        appendSubmitAudit(assignment, submission, agentRun.getId());
        aiReviewDispatcher.enqueue(submission.getId());
        datasetClaimService.increaseSubmittedCount(submission.getDatasetItemId());
        return toResponse(submission, agentRun.getId());
    }

    private Assignment loadOwnedAssignment(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, labelerId);
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    private Task loadSubmittableTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != TaskStatus.PUBLISHED
                || task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(TASK_NOT_SUBMITTABLE, "Task is not submittable");
        }
        return task;
    }

    private void requireCurrentDraftVersion(Assignment assignment, Integer clientDraftVersion) {
        if (!Objects.equals(assignment.getDraftVersion(), clientDraftVersion)) {
            throw new BusinessException(DRAFT_VERSION_CONFLICT, "Draft version conflict");
        }
    }

    private void requireSubmittableStatus(Assignment assignment) {
        if (!SUBMITTABLE_STATUSES.contains(assignment.getStatus())) {
            throw new BusinessException(ASSIGNMENT_STATUS_NOT_SUBMITTABLE, "Assignment status is not submittable");
        }
    }

    private String canonicalAnswerJson(String answerJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(answerJson);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(INVALID_ANSWER_JSON, "Answer JSON is invalid");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Submission createSubmission(Assignment assignment,
                                        String canonicalAnswerJson,
                                        String answerHash,
                                        int versionNo) {
        Submission submission = new Submission();
        submission.setAssignmentId(assignment.getId());
        submission.setTaskId(assignment.getTaskId());
        submission.setDatasetItemId(assignment.getDatasetItemId());
        submission.setLabelerId(assignment.getLabelerId());
        submission.setTemplateVersionId(assignment.getTemplateVersionId());
        submission.setVersionNo(versionNo);
        submission.setAnswerJson(canonicalAnswerJson);
        submission.setAnswerHash(answerHash);
        submission.setStatus(SubmissionStatus.AI_REVIEWING);
        submissionMapper.insert(submission);
        return submission;
    }

    private AgentRun createPendingAiReviewRun(Submission submission, Task task) {
        AgentRun agentRun = new AgentRun();
        agentRun.setAgentType(AI_REVIEW_AGENT_TYPE);
        agentRun.setSubmissionId(submission.getId());
        agentRun.setStatus(AgentRunStatus.PENDING);
        agentRun.setInputSnapshot(agentRunInputSnapshot(submission, task));
        agentRunMapper.insert(agentRun);
        return agentRun;
    }

    private String agentRunInputSnapshot(Submission submission, Task task) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", submission.getId());
        snapshot.put("assignmentId", submission.getAssignmentId());
        snapshot.put("taskId", submission.getTaskId());
        snapshot.put("aiReviewConfigId", task.getAiReviewConfigId());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void appendSubmitAudit(Assignment assignment, Submission submission, Long agentRunId) {
        Map<String, Object> beforeJson = new LinkedHashMap<>();
        beforeJson.put("assignmentId", assignment.getId());
        beforeJson.put("status", assignment.getStatus());
        beforeJson.put("draftVersion", assignment.getDraftVersion());

        Map<String, Object> afterJson = new LinkedHashMap<>();
        afterJson.put("assignmentId", assignment.getId());
        afterJson.put("status", AssignmentStatus.SUBMITTED);
        afterJson.put("submissionId", submission.getId());
        afterJson.put("versionNo", submission.getVersionNo());
        afterJson.put("answerHash", submission.getAnswerHash());

        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, assignment.getLabelerId(),
                ASSIGNMENT_BIZ_TYPE, assignment.getId(),
                "ASSIGNMENT_SUBMITTED", beforeJson, afterJson, null, agentRunId));
    }

    private SubmissionSubmitResponse toResponse(Submission submission, Long agentRunId) {
        return new SubmissionSubmitResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getVersionNo(),
                submission.getStatus(),
                submission.getAnswerHash(),
                agentRunId
        );
    }
}
