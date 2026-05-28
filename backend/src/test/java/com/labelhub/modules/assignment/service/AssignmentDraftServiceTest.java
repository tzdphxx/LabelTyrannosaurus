package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentDraftServiceTest {

    private static final Long ASSIGNMENT_ID = 10L;
    private static final Long LABELER_ID = 20L;
    private static final String ANSWER_JSON = "{\"answer\":\"hello\"}";

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private AssignmentDraftCacheService assignmentDraftCacheService;

    @Mock
    private AuditAppender auditAppender;

    private AssignmentDraftService assignmentDraftService;

    @BeforeEach
    void setUp() {
        assignmentDraftService = new AssignmentDraftService(
                assignmentMapper,
                assignmentDraftCacheService,
                auditAppender,
                new ObjectMapper()
        );
    }

    @Test
    void savesDraftWhenClientVersionMatches() {
        Assignment assignment = assignment(AssignmentStatus.CLAIMED, 1, null);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(assignmentMapper.updateDraftIfVersionMatches(ASSIGNMENT_ID, LABELER_ID, ANSWER_JSON, 1, 2,
                AssignmentStatus.DRAFTING)).thenReturn(1);

        AssignmentDraftResponse response = assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest(ANSWER_JSON, 1)
        );

        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.draftAnswerJson()).isEqualTo(ANSWER_JSON);
        assertThat(response.draftVersion()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(AssignmentStatus.DRAFTING);
        assertThat(response.updatedAt()).isNotNull();
        verify(assignmentDraftCacheService).put(any(AssignmentDraftCacheEntry.class));
        verify(auditAppender).append(eq("ASSIGNMENT"), eq(ASSIGNMENT_ID), eq("USER"), eq(LABELER_ID),
                eq("ASSIGNMENT_DRAFT_SAVED"), any(), any(), eq(null), eq(null));
    }

    @Test
    void rejectsStaleClientVersionWithoutWritingCacheOrAudit() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2, ANSWER_JSON);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        assertThatThrownBy(() -> assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest("{\"answer\":\"new\"}", 1)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(409301));

        verify(assignmentMapper, never()).updateDraftIfVersionMatches(any(), any(), any(), any(), any(), any());
        verify(assignmentDraftCacheService, never()).put(any());
        verify(auditAppender, never()).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsConcurrentVersionConflictWhenConditionalUpdateMisses() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2, ANSWER_JSON);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(assignmentMapper.updateDraftIfVersionMatches(ASSIGNMENT_ID, LABELER_ID, "{\"answer\":\"new\"}", 2, 3,
                AssignmentStatus.DRAFTING)).thenReturn(0);

        assertThatThrownBy(() -> assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest("{\"answer\":\"new\"}", 2)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(409301));

        verify(assignmentDraftCacheService, never()).put(any());
        verify(auditAppender, never()).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMissingOrNonOwnedAssignment() {
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(null);

        assertThatThrownBy(() -> assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest(ANSWER_JSON, 1)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(404301));
    }

    @Test
    void rejectsNonEditableStatus() {
        Assignment assignment = assignment(AssignmentStatus.SUBMITTED, 1, ANSWER_JSON);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        assertThatThrownBy(() -> assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest(ANSWER_JSON, 1)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400301));

        verify(assignmentMapper, never()).updateDraftIfVersionMatches(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidAnswerJson() {
        Assignment assignment = assignment(AssignmentStatus.CLAIMED, 1, null);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        assertThatThrownBy(() -> assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest("{bad-json", 1)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(400302));

        verify(assignmentMapper, never()).updateDraftIfVersionMatches(any(), any(), any(), any(), any(), any());
    }

    @Test
    void readsDraftFromCacheWhenOwnedCacheEntryExists() {
        LocalDateTime updatedAt = LocalDateTime.now();
        when(assignmentDraftCacheService.get(ASSIGNMENT_ID))
                .thenReturn(Optional.of(new AssignmentDraftCacheEntry(
                        ASSIGNMENT_ID,
                        LABELER_ID,
                        ANSWER_JSON,
                        2,
                        AssignmentStatus.DRAFTING,
                        updatedAt
                )));

        AssignmentDraftResponse response = assignmentDraftService.getDraft(ASSIGNMENT_ID, LABELER_ID);

        assertThat(response.draftAnswerJson()).isEqualTo(ANSWER_JSON);
        assertThat(response.draftVersion()).isEqualTo(2);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        verify(assignmentMapper, never()).selectOwnedAssignment(any(), any());
    }

    @Test
    void readsDraftFromMysqlAndWarmsCacheWhenRedisMisses() {
        Assignment assignment = assignment(AssignmentStatus.DRAFTING, 2, ANSWER_JSON);
        when(assignmentDraftCacheService.get(ASSIGNMENT_ID)).thenReturn(Optional.empty());
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);

        AssignmentDraftResponse response = assignmentDraftService.getDraft(ASSIGNMENT_ID, LABELER_ID);

        assertThat(response.draftAnswerJson()).isEqualTo(ANSWER_JSON);
        assertThat(response.draftVersion()).isEqualTo(2);
        verify(assignmentDraftCacheService).put(any(AssignmentDraftCacheEntry.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void auditDoesNotStoreFullDraftAnswer() {
        Assignment assignment = assignment(AssignmentStatus.CLAIMED, 1, null);
        when(assignmentMapper.selectOwnedAssignment(ASSIGNMENT_ID, LABELER_ID)).thenReturn(assignment);
        when(assignmentMapper.updateDraftIfVersionMatches(ASSIGNMENT_ID, LABELER_ID, ANSWER_JSON, 1, 2,
                AssignmentStatus.DRAFTING)).thenReturn(1);

        assignmentDraftService.saveDraft(
                ASSIGNMENT_ID,
                LABELER_ID,
                new AssignmentDraftSaveRequest(ANSWER_JSON, 1)
        );

        ArgumentCaptor<Map<String, Object>> afterJsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditAppender).append(eq("ASSIGNMENT"), eq(ASSIGNMENT_ID), eq("USER"), eq(LABELER_ID),
                eq("ASSIGNMENT_DRAFT_SAVED"), any(), afterJsonCaptor.capture(), eq(null), eq(null));
        assertThat(afterJsonCaptor.getValue()).containsEntry("answerLength", ANSWER_JSON.length());
        assertThat(afterJsonCaptor.getValue()).doesNotContainKey("draftAnswerJson");
    }

    private Assignment assignment(AssignmentStatus status, Integer draftVersion, String draftAnswerJson) {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setLabelerId(LABELER_ID);
        assignment.setStatus(status);
        assignment.setDraftVersion(draftVersion);
        assignment.setDraftAnswerJson(draftAnswerJson);
        assignment.setUpdatedAt(LocalDateTime.now());
        return assignment;
    }
}
