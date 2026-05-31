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
class LlmProviderControllerTest {

    @Mock
    private LlmProviderService llmProviderService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void labelerCannotListProviders() {
        CurrentUserContext.set(labeler());
        LlmProviderController controller = new LlmProviderController(llmProviderService);

        assertThatThrownBy(controller::list)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCreateUsesCurrentUserId() {
        CurrentUserContext.set(admin());
        LlmProviderController controller = new LlmProviderController(llmProviderService);
        CreateLlmProviderRequest request = createRequest();
        LlmProviderResponse serviceResponse = response();
        when(llmProviderService.create(1L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderResponse> response = controller.create(request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).create(1L, request);
    }

    @Test
    void labelerCannotUpdateProvider() {
        CurrentUserContext.set(labeler());
        LlmProviderController controller = new LlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.update(10L, updateRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void labelerCannotTestProvider() {
        CurrentUserContext.set(labeler());
        LlmProviderController controller = new LlmProviderController(llmProviderService);

        assertThatThrownBy(() -> controller.test(10L, new TestLlmProviderRequest(null, "qwen-plus", Map.of())))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void adminCanTestProvider() {
        CurrentUserContext.set(admin());
        LlmProviderController controller = new LlmProviderController(llmProviderService);
        TestLlmProviderRequest request = new TestLlmProviderRequest(null, "qwen-plus", Map.of());
        LlmProviderTestResponse serviceResponse = new LlmProviderTestResponse(true, 12L, "OK");
        when(llmProviderService.test(10L, request)).thenReturn(serviceResponse);

        ApiResponse<LlmProviderTestResponse> response = controller.test(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(llmProviderService).test(10L, request);
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin", "admin@labelhub.dev", Set.of(RoleCode.ADMIN), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(2L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1);
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
                true,
                1L,
                null,
                null
        );
    }
}
