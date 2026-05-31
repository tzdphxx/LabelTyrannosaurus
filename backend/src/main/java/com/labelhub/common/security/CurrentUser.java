package com.labelhub.common.security;

import java.util.Set;

public record CurrentUser(Long userId, String username, String email, Set<RoleCode> roles, Integer tokenVersion) {
}
