package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @Schema(description = "旧密码", example = "OldPassword123", format = "password")
        @NotBlank @Size(min = 1) String oldPassword,
        @Schema(description = "新密码，8 到 128 字符", example = "NewPassword123", format = "password")
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
