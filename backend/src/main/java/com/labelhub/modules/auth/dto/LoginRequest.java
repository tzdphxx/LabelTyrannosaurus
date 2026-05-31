package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名或邮箱", example = "labeler")
        @NotBlank
        String account,
        @Schema(description = "登录密码", example = "Password123", format = "password")
        @NotBlank
        String password
) {
}
