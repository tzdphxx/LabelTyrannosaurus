package com.labelhub.modules.admin.dto;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员用户视图响应")
public record AdminUserResponse(
        @Schema(description = "用户 ID", example = "10")
        Long userId,
        @Schema(description = "用户名", example = "labeler")
        String username,
        @Schema(description = "邮箱", example = "user@example.com")
        String email,
        @Schema(description = "用户类型", example = "NORMAL")
        UserType userType,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "是否允许登录", example = "true")
        Boolean loginEnabled,
        @Schema(description = "令牌版本", example = "1")
        Integer tokenVersion,
        @Schema(description = "用户角色", example = "LABELER")
        RoleCode role
) {
}
