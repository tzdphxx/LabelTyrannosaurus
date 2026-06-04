package com.labelhub.modules.agent.service;

import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
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
        return create(agentType, submissionId, providerId, modelName, promptVersion, inputSnapshot, assignmentId, null);
    }

    @Transactional
    public AgentRun create(String agentType, Long submissionId, Long providerId,
                           String modelName, String promptVersion, String inputSnapshot,
                           Long assignmentId, String traceId) {
        AgentRun run = new AgentRun();
        run.setAgentType(agentType);
        run.setSubmissionId(submissionId);
        run.setAssignmentId(assignmentId);
        run.setProviderId(providerId);
        run.setModelName(modelName);
        run.setPromptVersion(promptVersion);
        run.setInputSnapshot(inputSnapshot);
        run.setTraceId(traceId);
        run.setQueuedAt(LocalDateTime.now());
        run.setStatus(AgentRunStatus.PENDING);
        agentRunMapper.insert(run);
        return run;
    }

    @Transactional
    public void start(Long agentRunId) {
        AgentRun run = requireRun(agentRunId);
        if (run.getStatus() != AgentRunStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot start AgentRun " + agentRunId + ": expected PENDING, got " + run.getStatus());
        }
        run.setStatus(AgentRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    /**
     * 按 ID 查询 AgentRun，仅当状态为 PENDING 时返回。
     * 用于幂等性检查：若已有未启动的 AgentRun，复用而非创建新的。
     *
     * @param agentRunId AgentRun ID，可为 null
     * @return PENDING 状态时返回该记录，否则返回 {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<AgentRun> findPending(Long agentRunId) {
        if (agentRunId == null) {
            return Optional.empty();
        }
        AgentRun run = agentRunMapper.selectById(agentRunId);
        return run != null && run.getStatus() == AgentRunStatus.PENDING
                ? Optional.of(run)
                : Optional.empty();
    }

    /**
     * 按 ID 查询 AgentRun，仅当状态为 PENDING 或 RUNNING 时返回。
     * 用于判断是否存在"活跃"的 AgentRun：若存在则复用；若不存在则需新建。
     *
     * @param agentRunId AgentRun ID，可为 null
     * @return PENDING 或 RUNNING 状态时返回该记录，否则返回 {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<AgentRun> findActive(Long agentRunId) {
        if (agentRunId == null) {
            return Optional.empty();
        }
        AgentRun run = agentRunMapper.selectById(agentRunId);
        return run != null
                && (run.getStatus() == AgentRunStatus.PENDING || run.getStatus() == AgentRunStatus.RUNNING)
                ? Optional.of(run)
                : Optional.empty();
    }

    @Transactional
    public void complete(Long agentRunId, String outputSnapshot) {
        AgentRun run = requireRun(agentRunId);
        if (run.getStatus() != AgentRunStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot complete AgentRun " + agentRunId + ": expected RUNNING, got " + run.getStatus());
        }
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setOutputSnapshot(outputSnapshot);
        run.setFinishedAt(LocalDateTime.now());
        run.setLatencyMs(calculateLatencyMs(run));
        agentRunMapper.updateById(run);
    }

    @Transactional
    public void fail(Long agentRunId, AgentRunStatus failStatus, String errorMessage) {
        if (!FAIL_STATUSES.contains(failStatus)) {
            throw new IllegalArgumentException(
                    "fail status must be FAILED, RATE_LIMITED or MANUAL_REQUIRED, got: " + failStatus);
        }
        AgentRun run = requireRun(agentRunId);
        if (run.getStatus() != AgentRunStatus.RUNNING && run.getStatus() != AgentRunStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot fail AgentRun " + agentRunId + ": expected PENDING or RUNNING, got " + run.getStatus());
        }
        run.setStatus(failStatus);
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(LocalDateTime.now());
        run.setLatencyMs(calculateLatencyMs(run));
        agentRunMapper.updateById(run);
    }

    private Long calculateLatencyMs(AgentRun run) {
        if (run.getStartedAt() == null || run.getFinishedAt() == null) {
            return null;
        }
        return Math.max(0L, Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis());
    }

    private AgentRun requireRun(Long agentRunId) {
        AgentRun run = agentRunMapper.selectById(agentRunId);
        if (run == null) {
            throw new IllegalStateException("AgentRun not found: " + agentRunId);
        }
        return run;
    }
}
