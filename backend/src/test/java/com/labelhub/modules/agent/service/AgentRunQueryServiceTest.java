package com.labelhub.modules.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentRunQueryServiceTest {

    @Mock private AgentRunMapper agentRunMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private TaskMapper taskMapper;

    private AgentRunQueryService service;

    @BeforeEach
    void setUp() {
        service = new AgentRunQueryService(
                agentRunMapper, submissionMapper, assignmentMapper, taskMapper,
                new AgentRunSnapshotRedactor(new ObjectMapper()));
    }

    @Test
    void ownerCanReadOwnTaskAgentRunWithSensitiveKeysRedacted() {
        stubRunGraph(30L, 40L, 10L, null);

        AgentRunDetailResponse response = service.getDetail(
                user(10L, RoleCode.OWNER), 1L);

        assertThat(response.agentRunId()).isEqualTo(1L);
        assertThat(response.redacted()).isFalse();
        assertThat(response.inputSnapshot()).containsEntry("prompt", "judge this");
        assertThat(response.inputSnapshot()).containsEntry("apiKey", "***REDACTED***");
        assertThat(response.inputSnapshot()).containsEntry("headers", "***REDACTED***");
        assertThat(response.outputSnapshot()).containsEntry("rawResponse", "ok");
        assertThat(response.outputSnapshot()).containsEntry("token", "***REDACTED***");
    }

    @Test
    void ownerCannotReadOtherOwnersTaskAgentRun() {
        stubRunGraph(30L, 40L, 99L, null);

        assertForbidden(() -> service.getDetail(user(10L, RoleCode.OWNER), 1L));
    }

    @Test
    void reviewerCanReadUnassignedReviewPoolAgentRun() {
        stubRunGraph(30L, 40L, 10L, null);

        AgentRunDetailResponse response = service.getDetail(
                user(20L, RoleCode.REVIEWER), 1L);

        assertThat(response.redacted()).isFalse();
        assertThat(response.submissionId()).isEqualTo(30L);
    }

    @Test
    void reviewerCanReadAgentRunAssignedToSelf() {
        stubRunGraph(30L, 40L, 10L, 20L);

        AgentRunDetailResponse response = service.getDetail(
                user(20L, RoleCode.REVIEWER), 1L);

        assertThat(response.redacted()).isFalse();
        assertThat(response.submissionId()).isEqualTo(30L);
    }

    @Test
    void reviewerCannotReadAgentRunAssignedToAnotherReviewer() {
        stubRunGraph(30L, 40L, 10L, 21L);

        assertForbidden(() -> service.getDetail(user(20L, RoleCode.REVIEWER), 1L));
    }

    @Test
    void labelerCanReadOwnAgentRunAsRedactedSummary() {
        stubRunGraph(30L, 40L, 10L, null);

        AgentRunDetailResponse response = service.getDetail(
                user(50L, RoleCode.LABELER), 1L);

        assertThat(response.redacted()).isTrue();
        assertThat(response.inputSnapshot()).containsEntry("promptMode", "IMAGE_SINGLE");
        assertThat(response.inputSnapshot()).containsEntry("degraded", false);
        assertThat(response.inputSnapshot()).containsEntry("apiKey", "***REDACTED***");
        assertThat(response.inputSnapshot()).doesNotContainKeys("prompt", "messages", "rawResponse");
        assertThat(response.outputSnapshot()).containsEntry("limitations", "none");
        assertThat(response.outputSnapshot()).doesNotContainKeys("output", "raw_response");
    }

    @Test
    void labelerCannotReadOtherLabelersAgentRun() {
        stubRunGraph(30L, 40L, 10L, null);

        assertForbidden(() -> service.getDetail(user(51L, RoleCode.LABELER), 1L));
    }

    @Test
    void missingAgentRunReturnsNotFound() {
        when(agentRunMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(user(10L, RoleCode.OWNER), 404L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404701));
    }

    @Test
    void invalidJsonSnapshotFallsBackToRawValue() {
        AgentRun run = run(1L, 30L);
        run.setInputSnapshot("not-json");
        run.setOutputSnapshot(null);
        when(agentRunMapper.selectById(1L)).thenReturn(run);
        stubSubmissionGraph(30L, 40L, 10L, null);

        AgentRunDetailResponse response = service.getDetail(
                user(10L, RoleCode.OWNER), 1L);

        assertThat(response.inputSnapshot()).containsEntry("raw", "not-json");
    }

    private void stubRunGraph(Long submissionId, Long taskId, Long ownerId, Long assignedReviewerId) {
        when(agentRunMapper.selectById(1L)).thenReturn(run(1L, submissionId));
        stubSubmissionGraph(submissionId, taskId, ownerId, assignedReviewerId);
    }

    private void stubSubmissionGraph(Long submissionId, Long taskId, Long ownerId, Long assignedReviewerId) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setAssignmentId(70L);
        submission.setTaskId(taskId);
        submission.setLabelerId(50L);
        submission.setAssignedReviewerId(assignedReviewerId);
        when(submissionMapper.selectById(submissionId)).thenReturn(submission);

        Assignment assignment = new Assignment();
        assignment.setId(70L);
        assignment.setTaskId(taskId);
        assignment.setLabelerId(50L);
        when(assignmentMapper.selectById(70L)).thenReturn(assignment);

        Task task = new Task();
        task.setId(taskId);
        task.setOwnerId(ownerId);
        when(taskMapper.selectById(taskId)).thenReturn(task);
    }

    private AgentRun run(Long id, Long submissionId) {
        AgentRun run = new AgentRun();
        run.setId(id);
        run.setAgentType("AI_REVIEW");
        run.setSubmissionId(submissionId);
        run.setProviderId(3L);
        run.setModelName("qwen-plus");
        run.setPromptVersion("v1");
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setInputSnapshot("""
                {
                  "prompt": "judge this",
                  "messages": ["hidden"],
                  "promptMode": "IMAGE_SINGLE",
                  "degraded": false,
                  "apiKey": "secret",
                  "headers": {"Authorization": "Bearer abc"}
                }
                """);
        run.setOutputSnapshot("""
                {
                  "output": "hidden",
                  "rawResponse": "ok",
                  "raw_response": "hidden",
                  "limitations": "none",
                  "token": "abc"
                }
                """);
        run.setStartedAt(LocalDateTime.now().minusSeconds(5));
        run.setFinishedAt(LocalDateTime.now());
        run.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        return run;
    }

    private CurrentUser user(Long userId, RoleCode role) {
        return new CurrentUser(userId, role.name().toLowerCase(), "test@labelhub.dev", Set.of(role), 1);
    }

    private void assertForbidden(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403701));
    }
}
