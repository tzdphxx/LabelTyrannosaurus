package com.labelhub.modules.agent.service;

import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentRunService {

    private static final Set<AgentRunStatus> FAIL_STATUSES =
            Set.of(AgentRunStatus.FAILED, AgentRunStatus.RATE_LIMITED, AgentRunStatus.MANUAL_REQUIRED);

    private final AgentRunMapper agentRunMapper;

    public AgentRunService(AgentRunMapper agentRunMapper) {
        this.agentRunMapper = agentRunMapper;
    }

    @Transactional
    public AgentRun create(String agentType, Long submissionId, Long providerId,
                           String modelName, String promptVersion, String inputSnapshot) {
        return create(agentType, submissionId, providerId, modelName, promptVersion, inputSnapshot, null);
    }

    @Transactional
    public AgentRun create(String agentType, Long submissionId, Long providerId,
                           String modelName, String promptVersion, String inputSnapshot,
                           Long assignmentId) {
        AgentRun run = new AgentRun();
        run.setAgentType(agentType);
        run.setSubmissionId(submissionId);
        run.setAssignmentId(assignmentId);
        run.setProviderId(providerId);
        run.setModelName(modelName);
        run.setPromptVersion(promptVersion);
        run.setInputSnapshot(inputSnapshot);
        run.setStatus(AgentRunStatus.PENDING);
        agentRunMapper.insert(run);
        return run;
    }

    @Transactional
    public void start(Long agentRunId) {
        AgentRun run = requireRun(agentRunId);
        run.setStatus(AgentRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    @Transactional
    public void complete(Long agentRunId, String outputSnapshot) {
        AgentRun run = requireRun(agentRunId);
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setOutputSnapshot(outputSnapshot);
        run.setFinishedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    @Transactional
    public void fail(Long agentRunId, AgentRunStatus failStatus, String errorMessage) {
        if (!FAIL_STATUSES.contains(failStatus)) {
            throw new IllegalArgumentException(
                    "fail status must be FAILED, RATE_LIMITED or MANUAL_REQUIRED, got: " + failStatus);
        }
        AgentRun run = requireRun(agentRunId);
        run.setStatus(failStatus);
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    private AgentRun requireRun(Long agentRunId) {
        AgentRun run = agentRunMapper.selectById(agentRunId);
        if (run == null) {
            throw new IllegalStateException("AgentRun not found: " + agentRunId);
        }
        return run;
    }
}
