package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "更新个人资料请求")
public record UpdateProfileRequest(
        @Schema(description = "显示名称，最大 64 字符", example = "张三")
        @Size(max = 64) String displayName,
        @Schema(description = "邮箱地址", example = "user@example.com")
        @Email @Size(max = 255) String email
) {
}
