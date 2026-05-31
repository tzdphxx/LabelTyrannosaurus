package com.labelhub.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI labelHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LabelHub API")
                        .version("v1")
                        .description("LabelHub 数据标注平台后端接口文档。认证接口公开访问，业务接口默认使用 JWT Bearer Token 鉴权。")
                        .contact(new Contact().name("LabelHub Backend")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后复制 accessToken，格式为 Bearer <token>。")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
