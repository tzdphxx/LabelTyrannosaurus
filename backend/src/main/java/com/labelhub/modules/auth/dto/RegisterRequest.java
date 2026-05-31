package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record RegisterRequest(
        @Schema(description = "用户名，最大 64 字符", example = "labeler")
        @NotBlank @Size(max = 64) String username,
        @Schema(description = "邮箱地址", example = "labeler@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "登录密码，8 到 128 字符", example = "Password123", format = "password")
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
