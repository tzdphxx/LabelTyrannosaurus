package com.labelhub.common.security;

import java.util.Set;

public record CurrentUser(Long userId, String username, String email, Set<RoleCode> roles, Integer tokenVersion) {

    public boolean hasRole(RoleCode role) {
        return roles.contains(role) || roles.contains(RoleCode.ADMIN);
    }

    public boolean isAdmin() {
        return roles.contains(RoleCode.ADMIN);
    }
}
