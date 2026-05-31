package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.service.AssignmentClaimService;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private AssignmentClaimService assignmentClaimService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void claimUsesCurrentUserId() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        AssignmentController controller = new AssignmentController(assignmentClaimService);
        AssignmentClaimResponse serviceResponse = new AssignmentClaimResponse(
                30L, 40L, 50L, "{\"type\":\"object\"}", "{\"text\":\"hello\"}", null, 1);
        when(assignmentClaimService.claim(10L, 20L)).thenReturn(serviceResponse);

        ApiResponse<AssignmentClaimResponse> response = controller.claim(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(assignmentClaimService).claim(10L, 20L);
    }
}
