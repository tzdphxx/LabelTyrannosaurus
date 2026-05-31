package com.labelhub.modules.auth.dto;

import com.labelhub.common.security.RoleCode;

import java.util.Set;

public record UserProfileResponse(Long userId, String username, String email, Set<RoleCode> roles) {
}
