package com.labelhub.modules.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.admin.dto.AssignableReviewTaskResponse;
import com.labelhub.modules.admin.dto.AssignableReviewerResponse;
import com.labelhub.modules.admin.dto.AdminClaimedReviewTaskRow;
import com.labelhub.modules.admin.dto.ClaimedReviewTaskResponse;
import com.labelhub.modules.admin.dto.ReviewerProgressResponse;
import com.labelhub.modules.admin.mapper.AdminReviewAssignmentMapper;
import com.labelhub.modules.admin.service.AdminReviewAssignmentQueryService;
import com.labelhub.modules.task.domain.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminReviewAssignmentQueryServiceTest {

    private final AdminReviewAssignmentMapper mapper = org.mockito.Mockito.mock(AdminReviewAssignmentMapper.class);
    private final AdminReviewAssignmentQueryService service = new AdminReviewAssignmentQueryService(mapper);

    @Test
    void listAssignableTasksDefaultsToUnclaimedTaskLevels() {
        LocalDateTime deadline = LocalDateTime.of(2026, 6, 10, 10, 0);
        AssignableReviewTaskResponse task = new AssignableReviewTaskResponse(
                10L, "Image QA", TaskStatus.PUBLISHED, deadline, 1, 8L,
                false, null, null, true);
        when(mapper.countAssignableTasks(null, null, null, false)).thenReturn(1L);
        when(mapper.selectAssignableTasks(null, null, null, false, 0, 100)).thenReturn(List.of(task));

        var page = service.listAssignableTasks(null, " ", null, false, 0, 500);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(100);
        assertThat(page.total()).isEqualTo(1L);
        assertThat(page.items()).containsExactly(task);
        verify(mapper).countAssignableTasks(null, null, null, false);
        verify(mapper).selectAssignableTasks(null, null, null, false, 0, 100);
    }

    @Test
    void includeClaimedTasksCarriesClaimedReviewer() {
        AssignableReviewTaskResponse claimed = new AssignableReviewTaskResponse(
                11L, "Text QA", TaskStatus.PAUSED, null, 2, 3L,
                true, 88L, "reviewer-a", false);
        when(mapper.countAssignableTasks(11L, "Text", 2, true)).thenReturn(1L);
        when(mapper.selectAssignableTasks(11L, "Text", 2, true, 20, 20)).thenReturn(List.of(claimed));

        var page = service.listAssignableTasks(11L, " Text ", 2, true, 2, 20);

        assertThat(page.items()).singleElement()
                .extracting("claimedReviewerId", "available")
                .containsExactly(88L, false);
    }

    @Test
    void listAssignableReviewersReturnsEnabledReviewerLoad() {
        AssignableReviewerResponse reviewer = new AssignableReviewerResponse(
                7L, "reviewer", "reviewer@example.com", true, true,
                4L, 5L, 6L, 4L, new BigDecimal("60.00"));
        when(mapper.countAssignableReviewers("rev", true)).thenReturn(1L);
        when(mapper.selectAssignableReviewers("rev", true, 0, 10)).thenReturn(List.of(reviewer));

        var page = service.listAssignableReviewers("rev", true, 1, 10);

        assertThat(page.items()).containsExactly(reviewer);
        assertThat(page.items().get(0).approvalRate()).isEqualByComparingTo("60.00");
    }

    @Test
    void listReviewerProgressAttachesClaimedTasksAndCounts() {
        ReviewerProgressResponse base = new ReviewerProgressResponse(
                7L, "reviewer", "reviewer@example.com", true, true,
                4L, 5L, 10L, new BigDecimal("60.00"), 0L, List.of());
        ClaimedReviewTaskResponse claimedTask = new ClaimedReviewTaskResponse(
                10L, "Image QA", 1, 4L, LocalDateTime.of(2026, 6, 4, 9, 30));
        when(mapper.selectReviewerProgress("rev", true)).thenReturn(List.of(base));
        when(mapper.selectClaimedTasksByReviewerIds(List.of(7L))).thenReturn(List.of(new AdminClaimedReviewTaskRow(
                7L,
                claimedTask.taskId(),
                claimedTask.title(),
                claimedTask.reviewLevel(),
                claimedTask.pendingCount(),
                claimedTask.claimedAt())));

        var progress = service.listReviewerProgress(" rev ", true);

        assertThat(progress).singleElement()
                .satisfies(item -> {
                    assertThat(item.claimedTaskCount()).isEqualTo(1L);
                    assertThat(item.claimedTasks()).containsExactly(claimedTask);
                });
    }
}
