package com.labelhub.modules.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.AiReviewPromptTestRequest;
import com.labelhub.modules.ai.dto.AiReviewPromptTestResponse;
import com.labelhub.modules.ai.service.AiReviewConfigService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiReviewConfigControllerTest {

    @Mock
    private AiReviewConfigService aiReviewConfigService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenSavingConfig() {
        CurrentUserContext.set(new CurrentUser(1L, "owner", "test@labelhub.dev", Set.of(RoleCode.OWNER), 1));
        AiReviewConfigController controller = new AiReviewConfigController(aiReviewConfigService);
        AiReviewConfigRequest request = request();
        AiReviewConfigResponse serviceResponse = response();
        when(aiReviewConfigService.save(1L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<AiReviewConfigResponse> response = controller.save(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(aiReviewConfigService).save(1L, 10L, request);
    }

    @Test
    void readsCurrentUserWhenTestingPrompt() {
        CurrentUserContext.set(new CurrentUser(1L, "owner", "test@labelhub.dev", Set.of(RoleCode.OWNER), 1));
        AiReviewConfigController controller = new AiReviewConfigController(aiReviewConfigService);
        AiReviewPromptTestRequest request = new AiReviewPromptTestRequest(Map.of("item", "raw"), Map.of("answer", "ok"));
        AiReviewPromptTestResponse serviceResponse = new AiReviewPromptTestResponse(
                70L,
                LlmGatewayStatus.SUCCESS,
                "PASS",
                Map.of("decision", "PASS"),
                "{}",
                10L,
                null,
                null
        );
        when(aiReviewConfigService.testPrompt(1L, 10L, 20L, request)).thenReturn(serviceResponse);

        ApiResponse<AiReviewPromptTestResponse> response = controller.test(10L, 20L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(aiReviewConfigService).testPrompt(1L, 10L, 20L, request);
    }

    private AiReviewConfigRequest request() {
        return new AiReviewConfigRequest(
                30L,
                "qwen-plus",
                "Review JSON",
                List.of("accuracy"),
                new BigDecimal("90.00"),
                new BigDecimal("60.00"),
                Map.of("type", "object"),
                3,
                null, null, null, null, null, null
        );
    }

    private AiReviewConfigResponse response() {
        return new AiReviewConfigResponse(
                20L,
                10L,
                30L,
                "qwen-plus",
                "Review JSON",
                List.of("accuracy"),
                new BigDecimal("90.00"),
                new BigDecimal("60.00"),
                Map.of("type", "object"),
                "v1",
                3,
                null, null, null, null, null, null
        );
    }
}
