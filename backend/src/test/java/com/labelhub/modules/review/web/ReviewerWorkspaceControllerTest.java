package com.labelhub.modules.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ReviewerTaskItemPageResponse;
import com.labelhub.modules.review.dto.ReviewerTaskItemRow;
import com.labelhub.modules.review.dto.ReviewerTaskStatusSummary;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.review.service.ReviewerTaskItemQueryService;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewerWorkspaceControllerTest {

    @Mock private ReviewerSubmissionListMapper reviewerListMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private ReviewerTaskItemQueryService taskItemQueryService;

    private ReviewerWorkspaceController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewerWorkspaceController(
                reviewerListMapper, submissionMapper, reviewRecordMapper, taskItemQueryService);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void taskItemsDelegatesSafePaginationAndFiltersToService() {
        CurrentUserContext.set(new CurrentUser(7L, "reviewer", "reviewer@labelhub.dev",
                Set.of(RoleCode.REVIEWER), 1));
        ReviewerTaskItemRow row = new ReviewerTaskItemRow(
                101L, "q-101", "{\"text\":\"hello\"}", "{\"source\":\"manual\"}",
                "SUBMITTED", 201L, "SUBMITTED", 301L, "labeler-a",
                401L, 2, "PENDING_FINAL", LocalDateTime.of(2026, 6, 4, 10, 0),
                "SUCCESS", "MANUAL_REVIEW", "88.500", "[\"risk\"]", "人工复核",
                "PENDING", 1, "ASSIGN_REVIEWER", LocalDateTime.of(2026, 6, 4, 10, 5),
                true, true);
        ReviewerTaskItemPageResponse serviceResponse = new ReviewerTaskItemPageResponse(
                100L, "客服风险复核", "PUBLISHED", 1L,
                new ReviewerTaskStatusSummary(0L, 0L, 0L, 1L, 0L, 0L),
                new PageResponse<>(List.of(row), 1, 100, 1L));
        when(taskItemQueryService.queryTaskItems(
                100L, 7L, "SUBMITTED", "PENDING_FINAL", "MANUAL_REVIEW", "risk", 1, 100))
                .thenReturn(serviceResponse);

        ApiResponse<ReviewerTaskItemPageResponse> response = controller.taskItems(
                100L, "SUBMITTED", "PENDING_FINAL", "MANUAL_REVIEW", "risk", -2, 500);

        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().page().items()).containsExactly(row);
    }

    @Test
    void labelerCannotReadTaskItems() {
        CurrentUserContext.set(new CurrentUser(8L, "labeler", "labeler@labelhub.dev",
                Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.taskItems(
                100L, null, null, null, null, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }
}
