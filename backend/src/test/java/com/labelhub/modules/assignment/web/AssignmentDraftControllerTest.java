package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.service.AssignmentDraftService;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentDraftControllerTest {

    @Mock
    private AssignmentDraftService assignmentDraftService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenSavingDraft() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        AssignmentDraftController controller = new AssignmentDraftController(assignmentDraftService);
        AssignmentDraftSaveRequest request = new AssignmentDraftSaveRequest("{\"answer\":\"hello\"}", 1);
        AssignmentDraftResponse serviceResponse = response();
        when(assignmentDraftService.saveDraft(10L, 20L, request)).thenReturn(serviceResponse);

        ApiResponse<AssignmentDraftResponse> response = controller.saveDraft(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(assignmentDraftService).saveDraft(10L, 20L, request);
    }

    @Test
    void readsCurrentUserWhenGettingDraft() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        AssignmentDraftController controller = new AssignmentDraftController(assignmentDraftService);
        AssignmentDraftResponse serviceResponse = response();
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
