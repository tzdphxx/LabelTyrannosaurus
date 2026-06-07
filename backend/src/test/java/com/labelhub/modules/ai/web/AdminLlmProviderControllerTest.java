package com.labelhub.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.CreateLlmProviderRequest;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.dto.TestLlmProviderRequest;
import com.labelhub.modules.ai.dto.UpdateLlmProviderRequest;
import com.labelhub.modules.ai.service.LlmProviderService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLlmProviderControllerTest {

    @Mock
    private LlmProviderService llmProviderService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void adminCreateUsesCurrentUserId() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        CreateLlmProviderRequest request = createRequest();
        LlmProviderResponse serviceResponse = response();
        when(llmProviderService.create(9L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.create(request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).create(9L, request);
    }

    @Test
    void adminCanListAllProviders() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        LlmProviderResponse serviceResponse = response();
        when(llmProviderService.listAllForAdmin()).thenReturn(List.of(serviceResponse));

        ApiResponse<List<LlmProviderResponse>> response = controller.list();

        assertThat(response.data()).containsExactly(serviceResponse);
        verify(llmProviderService).listAllForAdmin();
    }

    @Test
    void ownerCannotCreateProvider() {
        CurrentUserContext.set(owner());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCanUpdateEnableDisableAndTestProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        UpdateLlmProviderRequest updateRequest = updateRequest();
        TestLlmProviderRequest testRequest = new TestLlmProviderRequest(null, "qwen-plus", Map.of());
        LlmProviderResponse providerResponse = response();
        LlmProviderTestResponse testResponse = new LlmProviderTestResponse(true, 12L, "OK");
        when(llmProviderService.update(9L, 10L, updateRequest)).thenReturn(providerResponse);
        when(llmProviderService.enable(9L, 10L)).thenReturn(providerResponse);
        when(llmProviderService.disable(9L, 10L)).thenReturn(providerResponse);
        when(llmProviderService.test(9L, 10L, testRequest)).thenReturn(testResponse);

        assertThat(controller.update(10L, updateRequest).data()).isEqualTo(providerResponse);
        assertThat(controller.enable(10L).data()).isEqualTo(providerResponse);
        assertThat(controller.disable(10L).data()).isEqualTo(providerResponse);
        assertThat(controller.test(10L, testRequest).data()).isEqualTo(testResponse);
        verify(llmProviderService).update(9L, 10L, updateRequest);
        verify(llmProviderService).enable(9L, 10L);
        verify(llmProviderService).disable(9L, 10L);
        verify(llmProviderService).test(9L, 10L, testRequest);
    }

    private CurrentUser admin() {
        return new CurrentUser(9L, "admin", "admin@labelhub.dev", Set.of(RoleCode.ADMIN), 1);
    }

    private CurrentUser owner() {
        return new CurrentUser(1L, "owner", "owner@labelhub.dev", Set.of(RoleCode.OWNER), 1);
    }

    private CreateLlmProviderRequest createRequest() {
        return new CreateLlmProviderRequest(
                "dashscope",
                "DashScope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "sk-test",
                "qwen-plus",
                Map.of(),
                60,
                30,
                10
        );
    }

    private UpdateLlmProviderRequest updateRequest() {
        return new UpdateLlmProviderRequest(
                "dashscope",
                "DashScope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                null,
                "qwen-plus",
                Map.of(),
                60,
                30,
                10
        );
    }

    private LlmProviderResponse response() {
        return new LlmProviderResponse(
                10L,
                "dashscope",
                "DashScope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-plus",
                Map.of(),
                true,
                60,
                30,
                10,
                false,
                false,
                10,
                null,
                "NONE",
                true,
                9L,
                null,
                null
        );
    }
}
