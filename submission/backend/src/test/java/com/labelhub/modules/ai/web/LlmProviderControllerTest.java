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
import com.labelhub.modules.ai.dto.LlmProviderResponse;
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
    void ownerCanListEnabledProviders() {
        CurrentUserContext.set(owner());
        LlmProviderController controller = new LlmProviderController(llmProviderService);
        LlmProviderResponse serviceResponse = response();
        when(llmProviderService.listEnabled()).thenReturn(List.of(serviceResponse));

        ApiResponse<List<LlmProviderResponse>> response = controller.list();

        assertThat(response.data()).containsExactly(serviceResponse);
        verify(llmProviderService).listEnabled();
    }

    @Test
    void labelerCannotListProviders() {
        CurrentUserContext.set(labeler());
        LlmProviderController controller = new LlmProviderController(llmProviderService);

        assertThatThrownBy(controller::list)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    private CurrentUser owner() {
        return new CurrentUser(1L, "owner", "owner@labelhub.dev", Set.of(RoleCode.OWNER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(2L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1);
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
                1L,
                null,
                null
        );
    }
}
