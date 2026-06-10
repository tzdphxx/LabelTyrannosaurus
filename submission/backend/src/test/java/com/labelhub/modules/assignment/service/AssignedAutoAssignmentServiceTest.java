package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.dataset.service.DatasetItemSnapshot;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.template.service.TemplateSchemaService;
import com.labelhub.modules.template.service.TemplateSchemaSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignedAutoAssignmentServiceTest {

    private static final Long TASK_ID = 10L;
    private static final Long LABELER_ID = 20L;
    private static final Long TEMPLATE_VERSION_ID = 40L;

    @Mock
    private DatasetClaimService datasetClaimService;

    @Mock
    private TemplateSchemaService templateSchemaService;

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private AuditAppender auditAppender;

    @Mock
    private TraceIdProvider traceIdProvider;

    @Test
    void autoClaimsEveryAvailableItemForAssignedLabeler() {
        AssignedAutoAssignmentService service = service();
        Task task = task();
        when(templateSchemaService.getTemplateSchema(TEMPLATE_VERSION_ID))
                .thenReturn(new TemplateSchemaSnapshot(TEMPLATE_VERSION_ID, "{}"));
        when(datasetClaimService.reserveClaimableItem(TASK_ID, LABELER_ID, 1))
                .thenReturn(
                        Optional.of(new DatasetItemSnapshot(101L, "{\"text\":\"one\"}")),
                        Optional.of(new DatasetItemSnapshot(102L, "{\"text\":\"two\"}")),
                        Optional.empty());
        AtomicLong assignmentId = new AtomicLong(500L);
        when(assignmentMapper.insert(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(assignmentId.getAndIncrement());
            return 1;
        });
        when(traceIdProvider.currentTraceId()).thenReturn("trace-auto");

        int claimedCount = service.autoClaimAll(task, LABELER_ID);

        assertThat(claimedCount).isEqualTo(2);
        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentMapper, times(2)).insert(assignmentCaptor.capture());
        List<Assignment> assignments = assignmentCaptor.getAllValues();
        assertThat(assignments).extracting(Assignment::getDatasetItemId).containsExactly(101L, 102L);
        assertThat(assignments).allSatisfy(assignment -> {
            assertThat(assignment.getTaskId()).isEqualTo(TASK_ID);
            assertThat(assignment.getLabelerId()).isEqualTo(LABELER_ID);
            assertThat(assignment.getTemplateVersionId()).isEqualTo(TEMPLATE_VERSION_ID);
            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.CLAIMED);
            assertThat(assignment.getDraftVersion()).isEqualTo(1);
        });
        verify(auditAppender, times(2)).append(any(AuditCommand.class));
    }

    @Test
    void returnsZeroWhenNoItemCanBeAutoClaimed() {
        AssignedAutoAssignmentService service = service();
        when(templateSchemaService.getTemplateSchema(TEMPLATE_VERSION_ID))
                .thenReturn(new TemplateSchemaSnapshot(TEMPLATE_VERSION_ID, "{}"));
        when(datasetClaimService.reserveClaimableItem(TASK_ID, LABELER_ID, 1))
                .thenReturn(Optional.empty());

        int claimedCount = service.autoClaimAll(task(), LABELER_ID);

        assertThat(claimedCount).isZero();
        verify(assignmentMapper, never()).insert(any(Assignment.class));
        verify(auditAppender, never()).append(any(AuditCommand.class));
    }

    private AssignedAutoAssignmentService service() {
        return new AssignedAutoAssignmentService(
                datasetClaimService,
                templateSchemaService,
                assignmentMapper,
                auditAppender,
                traceIdProvider);
    }

    private Task task() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOverlapCount(1);
        task.setPublishedTemplateVersionId(TEMPLATE_VERSION_ID);
        return task;
    }
}
