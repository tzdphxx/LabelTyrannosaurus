package com.labelhub.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void definesBearerJwtSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().labelHubOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("LabelHub API");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.BEARER_AUTH);
        assertThat(openAPI.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement)
                        .containsKey(OpenApiConfig.BEARER_AUTH));
    }
}
