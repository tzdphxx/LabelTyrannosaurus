package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record RefreshRequest(
        @Schema(description = "登录或刷新接口返回的 refreshToken", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank
        String refreshToken
) {
}
