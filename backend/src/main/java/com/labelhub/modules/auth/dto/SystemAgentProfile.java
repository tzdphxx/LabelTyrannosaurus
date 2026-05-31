package com.labelhub.modules.auth.dto;

import com.labelhub.common.security.RoleCode;

import java.util.Set;

public record SystemAgentProfile(Long userId, String username, Set<RoleCode> roles) {
}
