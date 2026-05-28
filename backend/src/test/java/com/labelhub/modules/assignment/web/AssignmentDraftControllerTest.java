package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.service.AssignmentDraftService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentDraftControllerTest {

    @Mock
    private AssignmentDraftService assignmentDraftService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Test
    void readsCurrentUserWhenSavingDraft() {
        AssignmentDraftController controller = new AssignmentDraftController(assignmentDraftService, currentUserContext);
        AssignmentDraftSaveRequest request = new AssignmentDraftSaveRequest("{\"answer\":\"hello\"}", 1);
        AssignmentDraftResponse serviceResponse = response();
        when(currentUserContext.currentUserId()).thenReturn(20L);
        when(assignmentDraftService.saveDraft(10L, 20L, request)).thenReturn(serviceResponse);

        ApiResponse<AssignmentDraftResponse> response = controller.saveDraft(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(assignmentDraftService).saveDraft(10L, 20L, request);
    }

    @Test
    void readsCurrentUserWhenGettingDraft() {
        AssignmentDraftController controller = new AssignmentDraftController(assignmentDraftService, currentUserContext);
        AssignmentDraftResponse serviceResponse = response();
        when(currentUserContext.currentUserId()).thenReturn(20L);
        when(assignmentDraftService.getDraft(10L, 20L)).thenReturn(serviceResponse);

        ApiResponse<AssignmentDraftResponse> response = controller.getDraft(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(assignmentDraftService).getDraft(10L, 20L);
    }

    private AssignmentDraftResponse response() {
        return new AssignmentDraftResponse(
                10L,
                "{\"answer\":\"hello\"}",
                2,
                AssignmentStatus.DRAFTING,
                LocalDateTime.now()
        );
    }
}
