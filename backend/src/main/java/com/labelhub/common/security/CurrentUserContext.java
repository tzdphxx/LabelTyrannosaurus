package com.labelhub.common.security;

import com.labelhub.common.exception.BusinessException;

import java.util.Optional;
import java.util.Set;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static CurrentUser requireCurrentUser() {
        return get().orElseThrow(() -> new BusinessException(401001, "Unauthorized"));
    }

    public static Long getUserId() {
        return requireCurrentUser().userId();
    }

    public static Set<RoleCode> getRoles() {
        return requireCurrentUser().roles();
    }

    public static CurrentUser requireRole(RoleCode role) {
        CurrentUser currentUser = requireCurrentUser();
        if (!currentUser.roles().contains(role)) {
            throw new BusinessException(403001, "Forbidden");
        }
        return currentUser;
    }

    public static CurrentUser requireAnyRole(Set<RoleCode> roles) {
        CurrentUser currentUser = requireCurrentUser();
        boolean matched = currentUser.roles().stream().anyMatch(roles::contains);
        if (!matched) {
            throw new BusinessException(403001, "Forbidden");
        }
        return currentUser;
    }

    public static Integer getTokenVersion() {
        return requireCurrentUser().tokenVersion();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
