package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.LabelerClaimedTaskResponse;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelerAssignmentQueryServiceTest {

    private static final Long LABELER_ID = 20L;
    private static final Long TASK_ID = 10L;

    @Mock
    private AssignmentMapper assignmentMapper;

    private LabelerAssignmentQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new LabelerAssignmentQueryService(assignmentMapper);
    }

    @Test
    void listsClaimedTasksWithOnlyCurrentLabelersItems() {
        when(assignmentMapper.selectLabelerClaimedTasks(LABELER_ID, 20, 0))
                .thenReturn(List.of(taskRow()));
        when(assignmentMapper.selectLabelerClaimedItems(LABELER_ID, TASK_ID, null, 20, 0))
                .thenReturn(List.of(itemRow(100L, "SUBMITTED")));

        List<LabelerClaimedTaskResponse> responses = queryService.listClaimedTasks(LABELER_ID, 1, 20);

        assertThat(responses).hasSize(1);
        LabelerClaimedTaskResponse task = responses.get(0);
        assertThat(task.taskId()).isEqualTo(TASK_ID);
        assertThat(task.title()).isEqualTo("QA task");
        assertThat(task.description()).isEqualTo("Check image quality");
        assertThat(task.itemsPreview()).hasSize(1);
        assertThat(task.itemsPreview().get(0).assignmentId()).isEqualTo(200L);
        assertThat(task.itemsPreview().get(0).datasetItemId()).isEqualTo(100L);
        assertThat(task.itemsPreview().get(0).assignmentStatus()).isEqualTo(AssignmentStatus.SUBMITTED);
        assertThat(task.itemsPreview().get(0).itemJson()).isEqualTo("{\"text\":\"mine\"}");
        assertThat(task.itemsPreview().get(0).latestSubmissionStatus()).isEqualTo("PENDING_FINAL");
    }

    @Test
    void getsClaimedTaskDetailWithStatusFilteredItems() {
        when(assignmentMapper.selectLabelerClaimedTask(LABELER_ID, TASK_ID)).thenReturn(taskRow());
        when(assignmentMapper.selectLabelerClaimedItems(LABELER_ID, TASK_ID, "DRAFTING", 10, 10))
                .thenReturn(List.of(itemRow(101L, "DRAFTING")));

        LabelerClaimedTaskResponse response = queryService.getClaimedTaskDetail(
                LABELER_ID, TASK_ID, "DRAFTING", 2, 10);

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.itemsPreview()).hasSize(1);
        assertThat(response.itemsPreview().get(0).assignmentStatus()).isEqualTo(AssignmentStatus.DRAFTING);
    }

    private Map<String, Object> taskRow() {
        return Map.ofEntries(
                Map.entry("task_id", TASK_ID),
                Map.entry("title", "QA task"),
                Map.entry("description", "Check image quality"),
                Map.entry("instruction_rich_text", "<p>Be precise</p>"),
                Map.entry("status", "PUBLISHED"),
                Map.entry("quota", 30),
                Map.entry("overlap_count", 2),
                Map.entry("deadline_at", LocalDateTime.of(2026, 6, 5, 10, 0)),
                Map.entry("published_template_version_id", 99L),
                Map.entry("claimed_item_count", 1L),
                Map.entry("updated_at", LocalDateTime.of(2026, 6, 3, 10, 0))
        );
    }

    private Map<String, Object> itemRow(Long datasetItemId, String status) {
        return Map.of(
                "assignment_id", 200L,
                "dataset_item_id", datasetItemId,
                "assignment_status", status,
                "item_json", "{\"text\":\"mine\"}",
                "metadata_json", "{\"source\":\"seed\"}",
                "draft_version", 3,
                "latest_submission_status", "PENDING_FINAL",
                "updated_at", LocalDateTime.of(2026, 6, 3, 11, 0)
        );
    }
}
