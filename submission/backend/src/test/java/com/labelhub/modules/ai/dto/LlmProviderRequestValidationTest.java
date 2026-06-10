package com.labelhub.modules.ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmProviderRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequestRequiresCoreFieldsAndApiKey() {
        CreateLlmProviderRequest request = new CreateLlmProviderRequest(
                " ",
                "",
                "",
                " ",
                "",
                Map.of(),
                -1,
                -1,
                -1
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("providerCode", "providerName", "baseUrl", "apiKey", "defaultModel",
                        "platformRateLimitPerMinute", "taskRateLimitPerMinute", "userRateLimitPerMinute");
    }

    @Test
    void updateRequestAllowsOmittedApiKeyButStillValidatesLimits() {
        UpdateLlmProviderRequest request = new UpdateLlmProviderRequest(
                "dashscope",
                "DashScope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                null,
                "qwen-plus",
                Map.of(),
                -1,
                null,
                0
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("platformRateLimitPerMinute");
    }
}
