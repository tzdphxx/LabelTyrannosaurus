package com.labelhub.modules.admin.dto;

import com.labelhub.common.security.RoleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新用户角色请求")
public record UpdateUserRolesRequest(
        @Schema(description = "用户角色", example = "REVIEWER",
                allowableValues = {"LABELER", "REVIEWER", "OWNER", "ADMIN"})
        @NotNull RoleCode role) {
}
