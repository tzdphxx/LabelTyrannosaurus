package com.labelhub.modules.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionSubmitServiceTest {

    private static final Long ASSIGNMENT_ID = 10L;
    private static final Long LABELER_ID = 20L;
    private static final Long TASK_ID = 30L;
    private static final Long DATASET_ITEM_ID = 40L;
    private static final Long TEMPLATE_VERSION_ID = 50L;
    private static final Long SUBMISSION_ID = 60L;
    private static final Long AGENT_RUN_ID = 70L;
    private static final String ANSWER_JSON = "{\"answer\":\"hello\"}";

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private AgentRunMapper agentRunMapper;

    @Mock
    private AnswerSchemaValidator answerSchemaValidator;

    @Mock
    private AuditAppender auditAppender;

    @Mock
    private AiReviewDispatcher aiReviewDispatcher;

    private SubmissionSubmitService submissionSubmitService;

    @BeforeEach
    void setUp() {
        submissionSubmitService = new SubmissionSubmitService(
                assignmentMapper,
                submissionMapper,
                taskMapper,
                agentRunMapper,
                answerSchemaValidator,
                auditAppender,
                aiReviewDispatcher
        );
    }

    @Test
    void submitsFirstVersionAndCreatesPendingAgentRunAndAudit() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask());
        when(submissionMapper.selectLatestByAssignmentId(ASSIGNMENT_ID)).thenReturn(null);
        when(submissionMapper.selectLatestActiveByAssignmentId(ASSIGNMENT_ID)).thenReturn(null);
        when(submissionMapper.insert(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(SUBMISSION_ID);
            return 1;
        });
        when(assignmentMapper.markSubmittedIfCurrent(ASSIGNMENT_ID, LABELER_ID, 2, AssignmentStatus.SUBMITTED))
                .thenReturn(1);
        when(agentRunMapper.insert(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun agentRun = invocation.getArgument(0);
            agentRun.setId(AGENT_RUN_ID);
            return 1;
        });

        SubmissionSubmitResponse response = submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 2)
        );

        assertThat(response.submissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.versionNo()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(SubmissionStatus.AI_REVIEWING);
        assertThat(response.answerHash()).hasSize(64);
        assertThat(response.agentRunId()).isEqualTo(AGENT_RUN_ID);
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).insert(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.AI_REVIEWING);
        assertThat(submissionCaptor.getValue().getVersionNo()).isEqualTo(1);
        assertThat(submissionCaptor.getValue().getAnswerJson()).isEqualTo("{\"answer\":\"hello\"}");
        ArgumentCaptor<AgentRun> agentRunCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(agentRunCaptor.capture());
        assertThat(agentRunCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.PENDING);
        assertThat(agentRunCaptor.getValue().getAgentType()).isEqualTo("AI_REVIEW");
        assertThat(agentRunCaptor.getValue().getSubmissionId()).isEqualTo(SUBMISSION_ID);
        verify(auditAppender).append(any(AuditCommand.class));
        verify(aiReviewDispatcher).enqueue(SUBMISSION_ID);
    }

    @Test
    void resubmitsReturnedAssignmentWithNextVersionAndSupersedesOldActiveSubmission() {
        Assignment assignment = assignment(AssignmentStatus.RETURNED, 3);
        Submission latest = submission(1, "different-hash", SubmissionStatus.REJECTED);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask());
        when(submissionMapper.selectLatestByAssignmentId(ASSIGNMENT_ID)).thenReturn(latest);
        when(submissionMapper.selectLatestActiveByAssignmentId(ASSIGNMENT_ID)).thenReturn(latest);
        when(submissionMapper.supersedeActiveByAssignmentId(ASSIGNMENT_ID)).thenReturn(1);
        when(submissionMapper.insert(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(SUBMISSION_ID);
            return 1;
        });
        when(assignmentMapper.markSubmittedIfCurrent(ASSIGNMENT_ID, LABELER_ID, 3, AssignmentStatus.SUBMITTED))
                .thenReturn(1);
        when(agentRunMapper.insert(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun agentRun = invocation.getArgument(0);
            agentRun.setId(AGENT_RUN_ID);
            return 1;
        });

        SubmissionSubmitResponse response = submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 3)
        );

        assertThat(response.versionNo()).isEqualTo(2);
        verify(submissionMapper).supersedeActiveByAssignmentId(ASSIGNMENT_ID);
    }

    @Test
    void duplicateSameAnswerReturnsExistingSubmissionWithoutNewInsertOrAgentRun() {
        Assignment assignment = assignment(AssignmentStatus.SUBMITTED, 2);
        Submission existing = submission(1, sha256("{\"answer\":\"hello\"}"), SubmissionStatus.AI_REVIEWING);
        existing.setId(SUBMISSION_ID);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask());
        when(submissionMapper.selectLatestActiveByAssignmentId(ASSIGNMENT_ID)).thenReturn(existing);

        SubmissionSubmitResponse response = submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest("{\n  \"answer\" : \"hello\"\n}", 2)
        );

        assertThat(response.submissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(response.versionNo()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(SubmissionStatus.AI_REVIEWING);
        assertThat(response.agentRunId()).isNull();
        verify(submissionMapper, never()).insert(any(Submission.class));
        verify(agentRunMapper, never()).insert(any(AgentRun.class));
        verify(auditAppender, never()).append(any(AuditCommand.class));
        verify(aiReviewDispatcher, never()).enqueue(any());
    }

    @Test
    void rejectsMissingOrNonOwnedAssignment() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(404401));
    }

    @Test
    void rejectsStaleDraftVersion() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 1)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(409101));
    }

    @Test
    void rejectsInvalidAnswerJson() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest("{bad-json", 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400402));
    }

    @Test
    void rejectsNonEditableStatusWhenAnswerIsNotDuplicate() {
        Assignment assignment = assignment(AssignmentStatus.APPROVED, 2);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask());
        when(submissionMapper.selectLatestActiveByAssignmentId(ASSIGNMENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400401));
    }

    @Test
    void rejectsNonPublishedTask() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2);
        Task task = publishedTask();
        task.setStatus(TaskStatus.PAUSED);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400403));
    }

    @Test
    void rejectsExpiredTask() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2);
        Task task = publishedTask();
        task.setDeadlineAt(LocalDateTime.now().minusMinutes(1));
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> submissionSubmitService.submit(
                ASSIGNMENT_ID,
                LABELER_ID,
                new SubmissionSubmitRequest(ANSWER_JSON, 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400403));
    }

    private Assignment assignment(AssignmentStatus status, Integer draftVersion) {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTaskId(TASK_ID);
        assignment.setDatasetItemId(DATASET_ITEM_ID);
        assignment.setLabelerId(LABELER_ID);
        assignment.setTemplateVersionId(TEMPLATE_VERSION_ID);
        assignment.setStatus(status);
        assignment.setDraftVersion(draftVersion);
        assignment.setDraftAnswerJson(ANSWER_JSON);
        return assignment;
    }

    private Task publishedTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        return task;
    }

    private Submission submission(Integer versionNo, String answerHash, SubmissionStatus status) {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setAssignmentId(ASSIGNMENT_ID);
        submission.setTaskId(TASK_ID);
        submission.setDatasetItemId(DATASET_ITEM_ID);
        submission.setLabelerId(LABELER_ID);
        submission.setTemplateVersionId(TEMPLATE_VERSION_ID);
        submission.setVersionNo(versionNo);
        submission.setAnswerJson(ANSWER_JSON);
        submission.setAnswerHash(answerHash);
        submission.setStatus(status);
        return submission;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
