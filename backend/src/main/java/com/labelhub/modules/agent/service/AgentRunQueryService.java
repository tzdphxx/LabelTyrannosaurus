package com.labelhub.modules.agent.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgentRunQueryService {

    private static final int AGENT_RUN_NOT_FOUND = 404701;
    private static final int FORBIDDEN = 403701;

    private final AgentRunMapper agentRunMapper;
    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final TaskMapper taskMapper;
    private final AgentRunSnapshotRedactor snapshotRedactor;

    public AgentRunQueryService(AgentRunMapper agentRunMapper,
                                SubmissionMapper submissionMapper,
                                AssignmentMapper assignmentMapper,
                                TaskMapper taskMapper,
                                AgentRunSnapshotRedactor snapshotRedactor) {
        this.agentRunMapper = agentRunMapper;
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.taskMapper = taskMapper;
        this.snapshotRedactor = snapshotRedactor;
    }

    public AgentRunDetailResponse getDetail(CurrentUser currentUser, Long agentRunId) {
        AgentRun run = agentRunMapper.selectById(agentRunId);
        if (run == null) {
            throw new BusinessException(AGENT_RUN_NOT_FOUND, "AgentRun not found");
        }
        Submission submission = run.getSubmissionId() == null ? null : submissionMapper.selectById(run.getSubmissionId());
        Assignment assignment = null;
        Task task = null;
        if (submission != null) {
            assignment = assignmentMapper.selectById(submission.getAssignmentId());
            task = taskMapper.selectById(submission.getTaskId());
        } else if (run.getAssignmentId() != null) {
            assignment = assignmentMapper.selectById(run.getAssignmentId());
            if (assignment != null) {
                task = taskMapper.selectById(assignment.getTaskId());
            }
        }
        if (submission == null && assignment == null) {
            throw new BusinessException(AGENT_RUN_NOT_FOUND, "AgentRun not found");
        }

        boolean labelerSummary = requireAccess(currentUser, submission, assignment, task);
        return toResponse(run, labelerSummary);
    }

    private boolean requireAccess(CurrentUser currentUser,
                                  Submission submission,
                                  Assignment assignment,
                                  Task task) {
        Set<RoleCode> roles = currentUser.roles();
        if (roles.contains(RoleCode.OWNER)
                && task != null
                && currentUser.userId().equals(task.getOwnerId())) {
            return false;
        }
        if (submission != null && roles.contains(RoleCode.REVIEWER)
                && (submission.getAssignedReviewerId() == null
                || currentUser.userId().equals(submission.getAssignedReviewerId()))) {
            return false;
        }
        if (roles.contains(RoleCode.LABELER)
                && assignment != null
                && currentUser.userId().equals(assignment.getLabelerId())) {
            return true;
        }
        throw new BusinessException(FORBIDDEN, "Forbidden");
    }

    private AgentRunDetailResponse toResponse(AgentRun run, boolean labelerSummary) {
        Map<String, Object> inputSnapshot = labelerSummary
                ? snapshotRedactor.labelerSummary(run.getInputSnapshot())
                : snapshotRedactor.full(run.getInputSnapshot());
        Map<String, Object> outputSnapshot = labelerSummary
                ? snapshotRedactor.labelerSummary(run.getOutputSnapshot())
                : snapshotRedactor.full(run.getOutputSnapshot());

        return new AgentRunDetailResponse(
                run.getId(),
                run.getAgentType(),
                run.getSubmissionId(),
                run.getAssignmentId(),
                run.getProviderId(),
                run.getModelName(),
                run.getPromptVersion(),
                run.getStatus(),
                inputSnapshot,
                outputSnapshot,
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getCreatedAt(),
                labelerSummary);
    }
}
