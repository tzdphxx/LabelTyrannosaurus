package com.labelhub.modules.admin.dto;

import com.labelhub.common.security.RoleCode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRolesRequest(@NotNull RoleCode role) {
}
