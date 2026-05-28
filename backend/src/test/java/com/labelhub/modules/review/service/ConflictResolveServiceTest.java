package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.domain.ConflictGroup;
import com.labelhub.modules.review.domain.ConflictStatus;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ConflictGroupResponse;
import com.labelhub.modules.review.dto.ConflictResolveRequest;
import com.labelhub.modules.review.dto.ConflictResolveResponse;
import com.labelhub.modules.review.mapper.ConflictGroupMapper;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConflictResolveServiceTest {

    private static final Long REVIEWER_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final Long TASK_ID = 100L;
    private static final Long ITEM_ID = 200L;

    @Mock private ConflictGroupMapper conflictGroupMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private SubmissionEventPublisher eventPublisher;
    @Mock private AuditAppender auditAppender;

    private ConflictResolveService service;

    @BeforeEach
    void setUp() {
        service = new ConflictResolveService(
                conflictGroupMapper, submissionMapper, reviewRecordMapper,
                eventPublisher, auditAppender);
    }

    // --- resolve ---

    @Test
    void resolveSetsGoldenAndPublishesEvent() {
        ConflictGroup group = openGroup();
        Submission golden = submissionInGroup(50L);
        when(conflictGroupMapper.selectById(GROUP_ID)).thenReturn(group);
        when(submissionMapper.selectById(50L)).thenReturn(golden);

        ConflictResolveResponse response = service.resolve(
                GROUP_ID, REVIEWER_ID, new ConflictResolveRequest(50L, "Best quality"));

        assertThat(response.status()).isEqualTo(ConflictStatus.RESOLVED);
        assertThat(response.goldenSubmissionId()).isEqualTo(50L);
        assertThat(golden.getIsGolden()).isTrue();
        assertThat(golden.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(group.getStatus()).isEqualTo(ConflictStatus.RESOLVED);
        verify(eventPublisher).publishGoldenSelected(50L, REVIEWER_ID);
    }

    @Test
    void resolveWritesReviewRecord() {
        when(conflictGroupMapper.selectById(GROUP_ID)).thenReturn(openGroup());
        when(submissionMapper.selectById(50L)).thenReturn(submissionInGroup(50L));

        service.resolve(GROUP_ID, REVIEWER_ID, new ConflictResolveRequest(50L, "Best"));

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(com.labelhub.modules.review.domain.ReviewAction.RESOLVE_CONFLICT);
        assertThat(captor.getValue().getReason()).isEqualTo("Best");
    }

    @Test
    void resolveNotFoundThrows() {
        when(conflictGroupMapper.selectById(GROUP_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.resolve(
                GROUP_ID, REVIEWER_ID, new ConflictResolveRequest(50L, "reason")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404701));
    }

    @Test
    void resolveAlreadyResolvedThrows() {
        ConflictGroup group = openGroup();
        group.setStatus(ConflictStatus.RESOLVED);
        when(conflictGroupMapper.selectById(GROUP_ID)).thenReturn(group);

        assertThatThrownBy(() -> service.resolve(
                GROUP_ID, REVIEWER_ID, new ConflictResolveRequest(50L, "reason")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400701));
        verify(eventPublisher, never()).publishGoldenSelected(any(), any());
    }

    @Test
    void resolveSubmissionNotInGroupThrows() {
        ConflictGroup group = openGroup();
        Submission wrongSubmission = new Submission();
        wrongSubmission.setId(99L);
        wrongSubmission.setTaskId(999L);
        wrongSubmission.setDatasetItemId(999L);
        when(conflictGroupMapper.selectById(GROUP_ID)).thenReturn(group);
        when(submissionMapper.selectById(99L)).thenReturn(wrongSubmission);

        assertThatThrownBy(() -> service.resolve(
                GROUP_ID, REVIEWER_ID, new ConflictResolveRequest(99L, "reason")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400702));
    }

    // --- detectAndCreateConflict ---

    @Test
    void detectCreatesGroupWhenAnswersDiffer() {
        Submission s1 = submissionWithHash(1L, "hash_a");
        Submission s2 = submissionWithHash(2L, "hash_b");
        when(submissionMapper.selectPendingFinalByTaskAndItem(TASK_ID, ITEM_ID))
                .thenReturn(List.of(s1, s2));
        when(conflictGroupMapper.selectByTaskAndItem(TASK_ID, ITEM_ID)).thenReturn(null);

        service.detectAndCreateConflict(TASK_ID, ITEM_ID);

        ArgumentCaptor<ConflictGroup> captor = ArgumentCaptor.forClass(ConflictGroup.class);
        verify(conflictGroupMapper).insert((ConflictGroup) captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ConflictStatus.OPEN);
        assertThat(captor.getValue().getConsensusScore()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    void detectDoesNotCreateGroupWhenAllSame() {
        Submission s1 = submissionWithHash(1L, "same_hash");
        Submission s2 = submissionWithHash(2L, "same_hash");
        when(submissionMapper.selectPendingFinalByTaskAndItem(TASK_ID, ITEM_ID))
                .thenReturn(List.of(s1, s2));

        service.detectAndCreateConflict(TASK_ID, ITEM_ID);

        verify(conflictGroupMapper, never()).insert((ConflictGroup) any());
    }

    @Test
    void detectDoesNotCreateGroupWhenLessThanTwo() {
        Submission s1 = submissionWithHash(1L, "hash_a");
        when(submissionMapper.selectPendingFinalByTaskAndItem(TASK_ID, ITEM_ID))
                .thenReturn(List.of(s1));

        service.detectAndCreateConflict(TASK_ID, ITEM_ID);

        verify(conflictGroupMapper, never()).insert((ConflictGroup) any());
        verify(conflictGroupMapper, never()).updateById((ConflictGroup) any());
    }

    @Test
    void detectUpdatesExistingGroup() {
        Submission s1 = submissionWithHash(1L, "hash_a");
        Submission s2 = submissionWithHash(2L, "hash_b");
        Submission s3 = submissionWithHash(3L, "hash_a");
        ConflictGroup existing = openGroup();
        when(submissionMapper.selectPendingFinalByTaskAndItem(TASK_ID, ITEM_ID))
                .thenReturn(List.of(s1, s2, s3));
        when(conflictGroupMapper.selectByTaskAndItem(TASK_ID, ITEM_ID)).thenReturn(existing);

        service.detectAndCreateConflict(TASK_ID, ITEM_ID);

        verify(conflictGroupMapper, never()).insert((ConflictGroup) any());
        verify(conflictGroupMapper).updateById(existing);
        assertThat(existing.getConsensusScore()).isEqualByComparingTo(new BigDecimal("0.6667"));
    }

    // --- listOpenGroups ---

    @Test
    void listOpenGroupsDelegates() {
        ConflictGroup group = openGroup();
        when(conflictGroupMapper.selectOpenGroups()).thenReturn(List.of(group));

        List<ConflictGroupResponse> result = service.listOpenGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).groupId()).isEqualTo(GROUP_ID);
    }

    // --- helpers ---

    private ConflictGroup openGroup() {
        ConflictGroup g = new ConflictGroup();
        g.setId(GROUP_ID);
        g.setTaskId(TASK_ID);
        g.setDatasetItemId(ITEM_ID);
        g.setStatus(ConflictStatus.OPEN);
        g.setConsensusScore(new BigDecimal("0.5000"));
        g.setCreatedAt(LocalDateTime.now());
        return g;
    }

    private Submission submissionInGroup(Long id) {
        Submission s = new Submission();
        s.setId(id);
        s.setTaskId(TASK_ID);
        s.setDatasetItemId(ITEM_ID);
        s.setStatus(SubmissionStatus.PENDING_FINAL);
        return s;
    }

    private Submission submissionWithHash(Long id, String hash) {
        Submission s = new Submission();
        s.setId(id);
        s.setTaskId(TASK_ID);
        s.setDatasetItemId(ITEM_ID);
        s.setAnswerHash(hash);
        s.setStatus(SubmissionStatus.PENDING_FINAL);
        return s;
    }
}