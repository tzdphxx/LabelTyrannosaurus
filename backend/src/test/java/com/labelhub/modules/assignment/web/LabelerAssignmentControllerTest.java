package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerClaimedTaskResponse;
import com.labelhub.modules.assignment.service.LabelerAssignmentQueryService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelerAssignmentControllerTest {

    @Mock
    private LabelerAssignmentQueryService queryService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenListingClaimedTasks() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        when(queryService.listClaimedTasks(20L, 1, 20)).thenReturn(List.of());
        LabelerClaimedTaskController controller = new LabelerClaimedTaskController(queryService);

        ApiResponse<List<LabelerClaimedTaskResponse>> response = controller.listClaimedTasks(1, 20);

        assertThat(response.data()).isEmpty();
        verify(queryService).listClaimedTasks(20L, 1, 20);
    }

    @Test
    void readsCurrentUserWhenGettingClaimedTaskDetail() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        LabelerClaimedTaskResponse task = new LabelerClaimedTaskResponse(
                10L, "QA task", null, null, null, null, null, null, null, 0L, null, List.of());
        when(queryService.getClaimedTaskDetail(20L, 10L, "DRAFTING", 2, 10)).thenReturn(task);
        LabelerClaimedTaskController controller = new LabelerClaimedTaskController(queryService);

        ApiResponse<LabelerClaimedTaskResponse> response = controller.getClaimedTaskDetail(10L, "DRAFTING", 2, 10);

        assertThat(response.data()).isEqualTo(task);
        verify(queryService).getClaimedTaskDetail(20L, 10L, "DRAFTING", 2, 10);
    }
}
