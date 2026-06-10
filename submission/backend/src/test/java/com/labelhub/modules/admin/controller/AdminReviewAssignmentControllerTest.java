package com.labelhub.modules.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.AssignableReviewTaskResponse;
import com.labelhub.modules.admin.dto.AssignableReviewerResponse;
import com.labelhub.modules.admin.dto.ReviewerProgressResponse;
import com.labelhub.modules.admin.service.AdminReviewAssignmentQueryService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminReviewAssignmentControllerTest {

    private final AdminReviewAssignmentQueryService service =
            org.mockito.Mockito.mock(AdminReviewAssignmentQueryService.class);
    private final AdminReviewAssignmentController controller = new AdminReviewAssignmentController(service);

    @BeforeEach
    void setUp() {
        CurrentUserContext.set(new CurrentUser(1L, "admin", "admin@example.com", Set.of(RoleCode.ADMIN), 1));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void adminCanListAssignableTasks() {
        PageResponse<AssignableReviewTaskResponse> page = new PageResponse<>(List.of(), 1, 20, 0);
        when(service.listAssignableTasks(null, null, null, false, 1, 20)).thenReturn(page);

        var response = controller.listAssignableTasks(null, null, null, false, 1, 20);

        assertThat(response.data()).isEqualTo(page);
    }

    @Test
    void adminCanListAssignableReviewers() {
        PageResponse<AssignableReviewerResponse> page = new PageResponse<>(List.of(), 1, 20, 0);
        when(service.listAssignableReviewers(null, true, 1, 20)).thenReturn(page);

        var response = controller.listAssignableReviewers(null, true, 1, 20);

        assertThat(response.data()).isEqualTo(page);
    }

    @Test
    void adminCanListReviewerProgress() {
        when(service.listReviewerProgress(null, true)).thenReturn(List.of());

        var response = controller.listReviewerProgress(null, true);

        assertThat(response.data()).isEmpty();
    }

    @Test
    void nonAdminCannotListAssignableTasks() {
        CurrentUserContext.set(new CurrentUser(2L, "reviewer", "r@example.com", Set.of(RoleCode.REVIEWER), 1));

        assertThatThrownBy(() -> controller.listAssignableTasks(null, null, null, false, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void controllerDelegatesPaginationToService() {
        controller.listAssignableTasks(10L, "qa", 1, true, -3, 999);

        verify(service).listAssignableTasks(10L, "qa", 1, true, -3, 999);
    }
}
