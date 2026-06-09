package com.labelhub.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
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
    void labelerTriggersFromAssignment() {
        LlmTriggerController controller = new LlmTriggerController(llmTriggerService);
        CurrentUser currentUser = new CurrentUser(2L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1);
        CurrentUserContext.set(currentUser);
        LlmTriggerRunRequest request = request();
        LlmTriggerRunResponse serviceResponse = response();
        when(llmTriggerService.runForAssignment(currentUser, 40L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmTriggerRunResponse> response = controller.runForAssignment(40L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmTriggerService).runForAssignment(currentUser, 40L, request);
    }

    @Test
    void ownerTestsFromTask() {
        LlmTriggerController controller = new LlmTriggerController(llmTriggerService);
        CurrentUser currentUser = new CurrentUser(1L, "owner", "test@labelhub.dev", Set.of(RoleCode.OWNER), 1);
        CurrentUserContext.set(currentUser);
        LlmTriggerRunRequest request = requestWithItem();
        LlmTriggerRunResponse serviceResponse = response();
        when(llmTriggerService.testFromTask(currentUser, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmTriggerRunResponse> response = controller.testFromTask(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmTriggerService).testFromTask(currentUser, 10L, request);
    }

    private LlmTriggerRunRequest request() {
        return new LlmTriggerRunRequest(null, null, null,
                null, null, 20L, Map.of(), null);
    }

    private LlmTriggerRunRequest requestWithItem() {
        return new LlmTriggerRunRequest(null, null, null,
                null, 30L, 20L, Map.of("answer", "draft"), null);
    }

    private LlmTriggerRunResponse response() {
        return new LlmTriggerRunResponse(70L, Map.of("suggestion", "ok"), "ok",
                List.of("summary"), "raw", LlmGatewayStatus.SUCCESS, 12L, null, null);
    }
}
