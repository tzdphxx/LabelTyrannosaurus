package com.labelhub.modules.admin.dto;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserType;

public record AdminUserResponse(
        Long userId,
        String username,
        String email,
        UserType userType,
        Boolean enabled,
        Boolean loginEnabled,
        Integer tokenVersion,
        RoleCode role
) {
}
