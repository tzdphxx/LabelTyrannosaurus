package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.dto.ReviewerTaskItemRow;
import com.labelhub.modules.review.dto.ReviewerTaskStatusCount;
import com.labelhub.modules.review.mapper.ReviewerTaskItemMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewerTaskItemQueryServiceTest {

    @Mock private ReviewerTaskItemMapper reviewerTaskItemMapper;
    @Mock private TaskMapper taskMapper;

    private ReviewerTaskItemQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReviewerTaskItemQueryService(reviewerTaskItemMapper, taskMapper);
    }

    @Test
    void queryTaskItemsReturnsPageAndStatusSummaryWhenReviewerHasTaskAccess() {
        Task task = new Task();
        task.setId(100L);
        task.setTitle("客服风险复核");
        task.setStatus(TaskStatus.PUBLISHED);
        ReviewerTaskItemRow row = new ReviewerTaskItemRow(
                101L, "q-101", "{\"text\":\"hello\"}", "{}",
                "SUBMITTED", 201L, "SUBMITTED", 301L, "labeler-a",
                401L, 1, "PENDING_FINAL", LocalDateTime.of(2026, 6, 4, 10, 0),
                "SUCCESS", "MANUAL_REVIEW", "91.000", "[]", "人工复核",
                "PENDING", 1, "ASSIGN_REVIEWER", LocalDateTime.of(2026, 6, 4, 10, 1),
                true, true);
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(reviewerTaskItemMapper.countTaskReviewerAccess(100L, 7L)).thenReturn(1);
        when(reviewerTaskItemMapper.countTaskItems(100L, "SUBMITTED", null, null, "risk", true))
                .thenReturn(1L);
        when(reviewerTaskItemMapper.selectTaskItems(100L, 7L, "SUBMITTED", null, null, "risk", true, 20, 20))
                .thenReturn(List.of(row));
        when(reviewerTaskItemMapper.selectStatusCounts(100L)).thenReturn(List.of(
                new ReviewerTaskStatusCount("UNCLAIMED", 2L),
                new ReviewerTaskStatusCount("SUBMITTED", 1L),
                new ReviewerTaskStatusCount("APPROVED", 3L)
        ));

        var response = service.queryTaskItems(100L, 7L, " SUBMITTED ", " ", null, " risk ", true, 2, 20);

        assertThat(response.taskId()).isEqualTo(100L);
        assertThat(response.taskTitle()).isEqualTo("客服风险复核");
        assertThat(response.taskStatus()).isEqualTo("PUBLISHED");
        assertThat(response.totalItemCount()).isEqualTo(6L);
        assertThat(response.statusSummary().unclaimedCount()).isEqualTo(2L);
        assertThat(response.statusSummary().submittedCount()).isEqualTo(1L);
        assertThat(response.statusSummary().approvedCount()).isEqualTo(3L);
        assertThat(response.page().items()).containsExactly(row);
        assertThat(response.page().page()).isEqualTo(2);
        assertThat(response.page().pageSize()).isEqualTo(20);
        assertThat(response.page().total()).isEqualTo(1L);
    }

    @Test
    void queryTaskItemsRejectsReviewerWithoutTaskAccess() {
        Task task = new Task();
        task.setId(100L);
        task.setStatus(TaskStatus.PUBLISHED);
        when(taskMapper.selectById(100L)).thenReturn(task);

        assertThatThrownBy(() -> service.queryTaskItems(
                100L, 7L, null, null, null, null, false, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void queryTaskItemsReturnsNotFoundWhenTaskDoesNotExist() {
        when(taskMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.queryTaskItems(
                404L, 7L, null, null, null, null, false, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404001));
    }
}
