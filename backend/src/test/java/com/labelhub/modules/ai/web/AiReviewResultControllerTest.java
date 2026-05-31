package com.labelhub.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.service.AiReviewResultQueryService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewResultControllerTest {

    @Mock private AiReviewResultQueryService queryService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenQueryingSubmissionAiReviewResult() {
        AiReviewResultController controller = new AiReviewResultController(queryService);
        CurrentUser reviewer = new CurrentUser(1L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1);
        CurrentUserContext.set(reviewer);
        AiReviewResultResponse serviceResponse = new AiReviewResultResponse(
                100L, 70L, 80L, 30L, "qwen-plus", AiReviewStatus.SUCCESS, "PASS",
                "92.50", Map.of("accuracy", 95), "[]", "Looks good", null, null);
        when(queryService.getForSubmission(reviewer, 70L)).thenReturn(serviceResponse);

        ApiResponse<AiReviewResultResponse> response = controller.get(70L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(queryService).getForSubmission(reviewer, 70L);
    }
}
