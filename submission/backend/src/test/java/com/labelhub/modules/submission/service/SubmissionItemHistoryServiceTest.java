package com.labelhub.modules.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionItemHistoryServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private AgentRunMapper agentRunMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private UserMapper userMapper;
    @Mock private SubmissionUserResolver userResolver;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void labelerSeesOnlyOwnSubmissionsForSameItem() {
        CurrentUserContext.set(user(7L, RoleCode.LABELER));
        Submission requested = submission(100L, 20L, 10L, 30L, 7L, null, 1, 88L);
        Submission ownSecond = submission(101L, 21L, 10L, 30L, 7L, null, 1, 88L);
        when(submissionMapper.selectById(100L)).thenReturn(requested);
        when(submissionMapper.selectItemHistorySubmissions(10L, 30L, 7L))
                .thenReturn(List.of(requested, ownSecond));
        when(userResolver.resolveCreatorNames(List.of(requested, ownSecond)))
                .thenReturn(Map.of(7L, "Labeler A"));
        when(userResolver.effectiveCreatorId(requested)).thenReturn(7L);
        when(userResolver.effectiveCreatorId(ownSecond)).thenReturn(7L);
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L, 101L))).thenReturn(List.of());
        when(reviewRecordMapper.selectBySubmissionIds(List.of(100L, 101L))).thenReturn(List.of());

        SubmissionItemHistoryResponse response = service().getItemHistory(100L);

        assertThat(response.taskId()).isEqualTo(10L);
        assertThat(response.datasetItemId()).isEqualTo(30L);
        assertThat(response.histories()).extracting(SubmissionItemHistoryResponse.HistoryItem::submissionId)
                .containsExactly(100L, 101L);
        assertThat(response.histories()).extracting(SubmissionItemHistoryResponse.HistoryItem::submittedBy)
                .containsExactly(7L, 7L);
        verify(submissionMapper).selectItemHistorySubmissions(10L, 30L, 7L);
    }

    @Test
    void labelerCannotRequestAnotherUsersSubmission() {
        CurrentUserContext.set(user(7L, RoleCode.LABELER));
        when(submissionMapper.selectById(100L))
                .thenReturn(submission(100L, 20L, 10L, 30L, 8L, null, 1, 88L));

        assertThatThrownBy(() -> service().getItemHistory(100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403501));

        verify(submissionMapper, never()).selectItemHistorySubmissions(10L, 30L, 7L);
    }

    @Test
    void reviewerSeesAllSubmissionsForItemWhenAssigned() {
        CurrentUserContext.set(user(30L, RoleCode.REVIEWER));
        Submission requested = submission(100L, 20L, 10L, 30L, 7L, null, 1, 30L);
        Submission otherLabeler = submission(102L, 22L, 10L, 30L, 8L, null, 1, 31L);
        when(submissionMapper.selectById(100L)).thenReturn(requested);
        when(submissionMapper.selectItemHistorySubmissions(10L, 30L, null))
                .thenReturn(List.of(requested, otherLabeler));
        when(userResolver.resolveCreatorNames(List.of(requested, otherLabeler)))
                .thenReturn(Map.of(7L, "Labeler A", 8L, "Labeler B"));
        when(userResolver.effectiveCreatorId(requested)).thenReturn(7L);
        when(userResolver.effectiveCreatorId(otherLabeler)).thenReturn(8L);
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L, 102L))).thenReturn(List.of());
        when(reviewRecordMapper.selectBySubmissionIds(List.of(100L, 102L))).thenReturn(List.of());

        SubmissionItemHistoryResponse response = service().getItemHistory(100L);

        assertThat(response.histories()).extracting(SubmissionItemHistoryResponse.HistoryItem::submittedBy)
                .containsExactly(7L, 8L);
        verify(submissionMapper).selectItemHistorySubmissions(10L, 30L, null);
    }

    @Test
    void reviewerCannotReadUnassignedSubmission() {
        CurrentUserContext.set(user(30L, RoleCode.REVIEWER));
        when(submissionMapper.selectById(100L))
                .thenReturn(submission(100L, 20L, 10L, 30L, 7L, null, 1, 31L));

        assertThatThrownBy(() -> service().getItemHistory(100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403601));

        verify(submissionMapper, never()).selectItemHistorySubmissions(10L, 30L, null);
    }

    @Test
    void aiReviewedAtPrefersAgentRunFinishedAtAndFallsBackToResultUpdatedAt() {
        CurrentUserContext.set(user(1L, RoleCode.OWNER));
        Submission first = submission(100L, 20L, 10L, 30L, 7L, null, 1, 30L);
        Submission second = submission(101L, 21L, 10L, 30L, 7L, null, 2, 30L);
        LocalDateTime runFinished = LocalDateTime.of(2026, 6, 8, 10, 1);
        LocalDateTime resultUpdated = LocalDateTime.of(2026, 6, 8, 10, 2);
        AiReviewResult withRun = aiResult(100L, 300L, 400L, resultUpdated);
        AiReviewResult withoutRun = aiResult(101L, 301L, null, resultUpdated.plusMinutes(1));
        AgentRun run = new AgentRun();
        run.setId(400L);
        run.setFinishedAt(runFinished);
        when(submissionMapper.selectById(100L)).thenReturn(first);
        when(submissionMapper.selectItemHistorySubmissions(10L, 30L, null))
                .thenReturn(List.of(first, second));
        when(userResolver.resolveCreatorNames(List.of(first, second))).thenReturn(Map.of(7L, "Labeler A"));
        when(userResolver.effectiveCreatorId(first)).thenReturn(7L);
        when(userResolver.effectiveCreatorId(second)).thenReturn(7L);
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L, 101L)))
                .thenReturn(List.of(withRun, withoutRun));
        when(agentRunMapper.selectBatchIds(List.of(400L))).thenReturn(List.of(run));
        when(reviewRecordMapper.selectBySubmissionIds(List.of(100L, 101L))).thenReturn(List.of());

        SubmissionItemHistoryResponse response = service().getItemHistory(100L);

        assertThat(response.histories().get(0).aiReview().reviewedAt()).isEqualTo(runFinished);
        assertThat(response.histories().get(1).aiReview().reviewedAt()).isEqualTo(resultUpdated.plusMinutes(1));
    }

    @Test
    void reviewRoundsAreGroupedAndSortedByLevelThenTime() {
        CurrentUserContext.set(user(1L, RoleCode.OWNER));
        Submission requested = submission(100L, 20L, 10L, 30L, 7L, null, 1, 30L);
        ReviewRecord level2 = reviewRecord(501L, 100L, 31L, 2,
                LocalDateTime.of(2026, 6, 8, 11, 0));
        ReviewRecord level1LaterInput = reviewRecord(500L, 100L, 30L, 1,
                LocalDateTime.of(2026, 6, 8, 10, 0));
        UserEntity reviewer30 = userEntity(30L, "reviewer30", "Reviewer 30");
        UserEntity reviewer31 = userEntity(31L, "reviewer31", "Reviewer 31");
        when(submissionMapper.selectById(100L)).thenReturn(requested);
        when(submissionMapper.selectItemHistorySubmissions(10L, 30L, null)).thenReturn(List.of(requested));
        when(userResolver.resolveCreatorNames(List.of(requested))).thenReturn(Map.of(7L, "Labeler A"));
        when(userResolver.effectiveCreatorId(requested)).thenReturn(7L);
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L))).thenReturn(List.of());
        when(reviewRecordMapper.selectBySubmissionIds(List.of(100L))).thenReturn(List.of(level2, level1LaterInput));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(reviewer30, reviewer31));

        SubmissionItemHistoryResponse response = service().getItemHistory(100L);

        assertThat(response.histories().get(0).reviewRounds())
                .extracting(SubmissionItemHistoryResponse.ReviewRoundHistory::reviewLevel)
                .containsExactly(1, 2);
        assertThat(response.histories().get(0).reviewRounds())
                .extracting(SubmissionItemHistoryResponse.ReviewRoundHistory::reviewerName)
                .containsExactly("Reviewer 30", "Reviewer 31");
    }

    private SubmissionItemHistoryService service() {
        return new SubmissionItemHistoryService(submissionMapper, aiReviewResultMapper,
                agentRunMapper, reviewRecordMapper, userMapper, userResolver);
    }

    private CurrentUser user(Long userId, RoleCode role) {
        return new CurrentUser(userId, "user" + userId, "u" + userId + "@test.dev", Set.of(role), 1);
    }

    private Submission submission(Long id, Long assignmentId, Long taskId, Long datasetItemId,
                                  Long labelerId, Long createdBy, int versionNo,
                                  Long assignedReviewerId) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setAssignmentId(assignmentId);
        submission.setTaskId(taskId);
        submission.setDatasetItemId(datasetItemId);
        submission.setLabelerId(labelerId);
        submission.setCreatedBy(createdBy);
        submission.setVersionNo(versionNo);
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submission.setSubmittedAt(LocalDateTime.of(2026, 6, 8, 9, 0).plusMinutes(id - 100L));
        submission.setAssignedReviewerId(assignedReviewerId);
        return submission;
    }

    private AiReviewResult aiResult(Long submissionId, Long id, Long runId, LocalDateTime updatedAt) {
        AiReviewResult result = new AiReviewResult();
        result.setId(id);
        result.setSubmissionId(submissionId);
        result.setEffectiveRunId(runId);
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setDecision("PASS");
        result.setUpdatedAt(updatedAt);
        result.setCreatedAt(updatedAt.minusMinutes(1));
        return result;
    }

    private ReviewRecord reviewRecord(Long id, Long submissionId, Long reviewerId,
                                      int level, LocalDateTime createdAt) {
        ReviewRecord record = new ReviewRecord();
        record.setId(id);
        record.setSubmissionId(submissionId);
        record.setReviewerId(reviewerId);
        record.setReviewLevel(level);
        record.setAction(ReviewAction.APPROVE);
        record.setReviewComment("ok");
        record.setCreatedAt(createdAt);
        return record;
    }

    private UserEntity userEntity(Long id, String username, String displayName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        return user;
    }
}
