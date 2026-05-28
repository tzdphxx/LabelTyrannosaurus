package com.labelhub.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.service.LlmTriggerService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmTriggerControllerTest {

    @Mock
    private LlmTriggerService llmTriggerService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void passesCurrentUserToService() {
        LlmTriggerController controller = new LlmTriggerController(llmTriggerService);
        CurrentUser currentUser = new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1);
        CurrentUserContext.set(currentUser);
        LlmTriggerRunRequest request = new LlmTriggerRunRequest(10L, 20L, "assist", 30L, 40L,
                Map.of("answer", "draft"), false);
        LlmTriggerRunResponse serviceResponse = new LlmTriggerRunResponse(60L, "assist",
                Map.of("suggestion", "ok"), "ok", List.of("answer"), "raw", LlmGatewayStatus.SUCCESS,
                12L, null, null);
        when(llmTriggerService.run(currentUser, request)).thenReturn(serviceResponse);

        ApiResponse<LlmTriggerRunResponse> response = controller.run(request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmTriggerService).run(currentUser, request);
    }
}
