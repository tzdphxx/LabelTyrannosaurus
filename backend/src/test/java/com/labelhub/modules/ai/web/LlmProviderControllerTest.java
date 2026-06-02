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
import com.labelhub.modules.ai.dto.OwnerModelOptionResponse;
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
class LlmProviderControllerTest {

    @Mock
    private LlmProviderService llmProviderService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    // ── Owner controller tests ──

    @Test
    void ownerCanListEnabledModels() {
        CurrentUserContext.set(owner());
        LlmProviderController controller = new LlmProviderController(llmProviderService);
        List<OwnerModelOptionResponse> serviceResponse = List.of(ownerModelOption());
        when(llmProviderService.listEnabledForOwner()).thenReturn(serviceResponse);

        ApiResponse<List<OwnerModelOptionResponse>> response = controller.list();

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).listEnabledForOwner();
    }

    @Test
    void labelerCannotListModels() {
        CurrentUserContext.set(labeler());
        LlmProviderController controller = new LlmProviderController(llmProviderService);

        assertThatThrownBy(controller::list)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    // ── Admin controller tests ──

    @Test
    void adminCanCreateProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        CreateLlmProviderRequest request = createRequest();
        LlmProviderResponse serviceResponse = adminResponse();
        when(llmProviderService.create(1L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.create(request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).create(1L, request);
    }

    @Test
    void ownerCannotCallAdminCreate() {
        CurrentUserContext.set(owner());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCanUpdateProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        UpdateLlmProviderRequest request = updateRequest();
        LlmProviderResponse serviceResponse = adminResponse();
        when(llmProviderService.update(1L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.update(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).update(1L, 10L, request);
    }

    @Test
    void ownerCannotCallAdminUpdate() {
        CurrentUserContext.set(owner());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.update(10L, updateRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCanEnableProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        LlmProviderResponse serviceResponse = adminResponse();
        when(llmProviderService.enable(1L, 10L)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.enable(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).enable(1L, 10L);
    }

    @Test
    void adminCanDisableProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        LlmProviderResponse serviceResponse = adminResponse();
        when(llmProviderService.disable(1L, 10L)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.disable(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).disable(1L, 10L);
    }

    @Test
    void adminCanTestProvider() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        TestLlmProviderRequest request = new TestLlmProviderRequest(null, "qwen-plus", Map.of());
        LlmProviderTestResponse serviceResponse = new LlmProviderTestResponse(true, 12L, "OK");
        when(llmProviderService.test(1L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderTestResponse> response = controller.test(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).test(1L, 10L, request);
    }

    @Test
    void ownerCannotCallAdminTest() {
        CurrentUserContext.set(owner());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.test(10L, new TestLlmProviderRequest(null, "qwen-plus", Map.of())))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCanListAllProviders() {
        CurrentUserContext.set(admin());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);
        List<LlmProviderResponse> serviceResponse = List.of(adminResponse());
        when(llmProviderService.listForAdmin()).thenReturn(serviceResponse);

        ApiResponse<List<LlmProviderResponse>> response = controller.list();

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).listForAdmin();
    }

    @Test
    void labelerCannotAccessAdminEndpoints() {
        CurrentUserContext.set(labeler());
        AdminLlmProviderController controller = new AdminLlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin", "admin@labelhub.dev", Set.of(RoleCode.ADMIN), 1);
    }

    private CurrentUser owner() {
        return new CurrentUser(2L, "owner", "owner@labelhub.dev", Set.of(RoleCode.OWNER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(3L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1);
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

    private OwnerModelOptionResponse ownerModelOption() {
        return new OwnerModelOptionResponse(
                10L, "dashscope", "DashScope", "qwen-plus",
                false, false, 10, null, "NONE"
        );
    }

    private LlmProviderResponse adminResponse() {
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
                null,
                true,
                null,
                1L,
                null,
                null
        );
    }
}
