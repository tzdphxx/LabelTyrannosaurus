package com.labelhub.modules.admin.dto;

import com.labelhub.common.security.RoleCode;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleCode> roles) {
}
