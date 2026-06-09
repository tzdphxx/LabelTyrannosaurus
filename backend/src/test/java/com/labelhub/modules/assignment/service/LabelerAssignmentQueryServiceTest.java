package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.assignment.dto.ClaimedTaskResponse;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelerAssignmentQueryServiceTest {

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private TaskTagMapper taskTagMapper;

    @Test
    void listClaimedTasksDoesNotLoadClaimedItems() {
        when(assignmentMapper.selectLabelerClaimedTasks(7L, 20, 0))
                .thenReturn(List.of(taskRow(10L)));
        when(taskTagMapper.selectByTaskIds(List.of(10L))).thenReturn(List.of());

        List<ClaimedTaskResponse> responses = service().listClaimedTasks(7L, 1, 20);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).task().taskId()).isEqualTo(10L);
        assertThat(responses.get(0).items()).isEmpty();
        verify(assignmentMapper, never()).selectLabelerClaimedItems(
                eq(7L), eq(10L), isNull(), anyInt(), anyInt());
    }

    @Test
    void getClaimedTaskDetailIncludesReturnedInfoOnItems() {
        LocalDateTime returnedAt = LocalDateTime.of(2026, 6, 8, 10, 30);
        when(assignmentMapper.selectLabelerClaimedTask(7L, 10L))
                .thenReturn(taskRow(10L));
        when(taskTagMapper.selectByTaskIds(List.of(10L))).thenReturn(List.of());
        when(assignmentMapper.selectLabelerClaimedItems(7L, 10L, "RETURNED", 20, 0))
                .thenReturn(List.of(Map.ofEntries(
                        Map.entry("assignment_id", 500L),
                        Map.entry("dataset_item_id", 100L),
                        Map.entry("external_id", "q1"),
                        Map.entry("assignment_status", "RETURNED"),
                        Map.entry("item_json", "{\"question\":\"...\"}"),
                        Map.entry("metadata_json", "{}"),
                        Map.entry("draft_version", 3),
                        Map.entry("latest_submission_status", "REJECTED"),
                        Map.entry("returned_reason", "missing required label"),
                        Map.entry("returned_at", returnedAt),
                        Map.entry("updated_at", returnedAt)
                )));

        ClaimedTaskResponse response = service().getClaimedTaskDetail(
                7L, 10L, "RETURNED", 1, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).claimId()).isEqualTo(500L);
        assertThat(response.items().get(0).returnedReason()).isEqualTo("missing required label");
        assertThat(response.items().get(0).returnedAt()).isEqualTo(returnedAt);
    }

    private LabelerAssignmentQueryService service() {
        return new LabelerAssignmentQueryService(assignmentMapper, taskTagMapper);
    }

    private Map<String, Object> taskRow(Long taskId) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 8, 9, 0);
        return Map.ofEntries(
                Map.entry("task_id", taskId),
                Map.entry("title", "Task " + taskId),
                Map.entry("status", TaskStatus.PUBLISHED.name()),
                Map.entry("quota", 100),
                Map.entry("claimed_count", 5),
                Map.entry("overlap_count", 1),
                Map.entry("strategy", ClaimStrategy.FCFS.name()),
                Map.entry("deadline_at", now.plusDays(1)),
                Map.entry("published_at", now.minusDays(1)),
                Map.entry("ended_at", now.plusDays(2)),
                Map.entry("created_at", now.minusDays(2)),
                Map.entry("updated_at", now),
                Map.entry("claimed_item_count", 2),
                Map.entry("submitted_count", 1),
                Map.entry("approved_count", 0)
        );
    }
}
