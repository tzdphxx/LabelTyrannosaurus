package com.labelhub.common.security;

import com.labelhub.common.exception.BusinessException;

import java.util.Optional;
import java.util.Set;

/**
 * 当前请求用户上下文。
 *
 * <p>JWT Filter 在请求进入时写入上下文，请求结束后清理。BE-A/BE-B 其他模块
 * 通过该类读取统一身份信息，避免重复解析 token。</p>
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    /**
     * 获取当前请求用户；未登录时返回空。
     */
    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    /**
     * 获取当前请求用户；未登录时抛出统一未认证错误。
     */
    public static CurrentUser requireCurrentUser() {
        return get().orElseThrow(() -> new BusinessException(401001, "Unauthorized"));
    }

    public static Long getUserId() {
        return requireCurrentUser().userId();
    }

    public static Set<RoleCode> getRoles() {
        return requireCurrentUser().roles();
    }

    public static Integer getTokenVersion() {
        return requireCurrentUser().tokenVersion();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
