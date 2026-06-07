package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.ai.domain.AiDecision;
import com.labelhub.modules.review.dto.ExportGoldenItem;
import com.labelhub.modules.review.dto.ExportPageRequest;
import com.labelhub.modules.review.dto.ExportPageResponse;
import com.labelhub.modules.review.mapper.ExportSubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportSnapshotServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long TASK_ID = 10L;

    @Mock private ExportSubmissionMapper exportSubmissionMapper;
    @Mock private TaskMapper taskMapper;

    private ExportSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new ExportSnapshotService(exportSubmissionMapper, taskMapper);
    }

    @Test
    void returnsGoldenItemsWithCursor() {
        Task task = ownedTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        List<ExportGoldenItem> items = List.of(
                goldenItem(100L), goldenItem(101L));
        when(exportSubmissionMapper.selectGoldenPage(TASK_ID, 0L, 51))
                .thenReturn(items);

        ExportPageResponse response = service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, null, 50));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).taskId()).isEqualTo(TASK_ID);
        assertThat(response.items().get(0).itemJsonRef()).isEqualTo("{\"text\":\"item\"}");
        assertThat(response.items().get(0).reviewSummary()).isEqualTo("final ok");
        assertThat(response.items().get(0).auditRef()).isEqualTo(50L);
        assertThat(response.nextCursor()).isEqualTo(101L);
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void hasMoreWhenExtraItemReturned() {
        Task task = ownedTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        List<ExportGoldenItem> items = List.of(
                goldenItem(100L), goldenItem(101L), goldenItem(102L));
        when(exportSubmissionMapper.selectGoldenPage(TASK_ID, 0L, 3))
                .thenReturn(items);

        ExportPageResponse response = service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, null, 2));

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(101L);
    }

    @Test
    void taskNotFoundThrows() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, null, 50)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404801));
    }

    @Test
    void notOwnerThrows() {
        Task task = ownedTask();
        task.setOwnerId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, null, 50)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403801));
    }

    @Test
    void emptyResultReturnsNullCursor() {
        Task task = ownedTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(exportSubmissionMapper.selectGoldenPage(TASK_ID, 0L, 51))
                .thenReturn(List.of());

        ExportPageResponse response = service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, null, 50));

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void cursorPaginationPassesLastId() {
        Task task = ownedTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(exportSubmissionMapper.selectGoldenPage(TASK_ID, 100L, 51))
                .thenReturn(List.of(goldenItem(101L)));

        ExportPageResponse response = service.queryExportableGoldenSubmissions(
                OWNER_ID, new ExportPageRequest(TASK_ID, 100L, 50));

        assertThat(response.items()).hasSize(1);
        assertThat(response.nextCursor()).isEqualTo(101L);
    }

    private Task ownedTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setOwnerId(OWNER_ID);
        return task;
    }

    private ExportGoldenItem goldenItem(Long id) {
        return new ExportGoldenItem(id, TASK_ID, 1L, "{\"text\":\"item\"}", 1L, 1, "{}",
                AiDecision.PASS, "ok", "final ok", 50L);
    }
}
