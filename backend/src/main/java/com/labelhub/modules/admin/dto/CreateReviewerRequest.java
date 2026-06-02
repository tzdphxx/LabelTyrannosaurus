package com.labelhub.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建审核员请求")
public record CreateReviewerRequest(
        @Schema(description = "用户名，最大 64 字符", example = "reviewer")
        @NotBlank @Size(max = 64) String username,
        @Schema(description = "邮箱地址", example = "reviewer@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "登录密码，8 到 128 字符", example = "Password123", format = "password")
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
