package com.labelhub.modules.auth.dto;

import com.labelhub.common.security.RoleCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "当前用户信息")
public record UserProfileResponse(
        @Schema(description = "用户 ID", example = "10")
        Long userId,
        @Schema(description = "用户名", example = "labeler")
        String username,
        @Schema(description = "邮箱", example = "labeler@example.com")
        String email,
        @Schema(description = "角色集合")
        Set<RoleCode> roles
) {
}
