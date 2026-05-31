package com.labelhub.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "令牌响应")
public record TokenResponse(
        @Schema(description = "访问令牌，默认 120 分钟过期", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "刷新令牌，默认 14 天过期", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "令牌版本，账号状态或角色变化后旧令牌失效", example = "1")
        Integer tokenVersion
) {
}
