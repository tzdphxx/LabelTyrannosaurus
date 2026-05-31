package com.labelhub.modules.submission.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.SubmissionSubmitRequest;
import com.labelhub.modules.submission.dto.SubmissionSubmitResponse;
import com.labelhub.modules.submission.service.SubmissionSubmitService;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmitControllerTest {

    @Mock
    private SubmissionSubmitService submissionSubmitService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenSubmittingAssignment() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        AssignmentSubmitController controller = new AssignmentSubmitController(submissionSubmitService);
        SubmissionSubmitRequest request = new SubmissionSubmitRequest("{\"answer\":\"hello\"}", 2);
        SubmissionSubmitResponse serviceResponse = new SubmissionSubmitResponse(
                60L,
                10L,
                1,
                SubmissionStatus.AI_REVIEWING,
                "hash",
                70L
        );
        when(submissionSubmitService.submit(10L, 20L, request)).thenReturn(serviceResponse);

        ApiResponse<SubmissionSubmitResponse> response = controller.submit(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(submissionSubmitService).submit(10L, 20L, request);
    }
}
