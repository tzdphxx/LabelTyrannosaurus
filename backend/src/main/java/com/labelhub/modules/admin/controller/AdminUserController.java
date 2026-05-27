package com.labelhub.modules.admin.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.admin.dto.AdminUserResponse;
import com.labelhub.modules.admin.dto.UpdateUserRolesRequest;
import com.labelhub.modules.admin.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin 用户管理接口入口。
 *
 * <p>类级别要求 {@code ADMIN} 角色。接口用于用户列表、角色调整和账号启停，
 * 任何会影响用户权限或登录态的操作都必须让旧 token 失效。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 查询后台用户列表。
     *
     * <p>默认排除 {@code userType=SYSTEM} 的系统用户，避免把 system principal
     * 暴露给普通后台用户管理流程。</p>
     *
     * @param includeSystem 是否包含系统用户，默认 false
     * @return 用户基础信息和角色集合
     */
    @GetMapping
    public ApiResponse<List<AdminUserResponse>> listUsers(@RequestParam(defaultValue = "false") boolean includeSystem) {
        return ApiResponse.ok(adminUserService.listUsers(includeSystem));
    }

    /**
     * 修改用户角色。
     *
     * <p>替换目标用户的完整角色集合，并递增 tokenVersion 使旧 token 失效。
     * 服务层会阻止移除系统中的最后一个 {@code ADMIN}。</p>
     *
     * @param userId 目标用户 id
     * @param request 新角色集合
     * @return 空响应体
     */
    @PutMapping("/{userId}/roles")
    public ApiResponse<Void> changeRoles(@PathVariable Long userId, @Valid @RequestBody UpdateUserRolesRequest request) {
        adminUserService.changeRoles(userId, request.roles());
        return ApiResponse.ok(null);
    }

    /**
     * 启用用户账号。
     *
     * <p>启用后用户可重新登录。底层更新会同步递增 tokenVersion，保证旧认证状态失效。</p>
     *
     * @param userId 目标用户 id
     * @return 空响应体
     */
    @PostMapping("/{userId}/enable")
    public ApiResponse<Void> enableUser(@PathVariable Long userId) {
        adminUserService.enableUser(userId);
        return ApiResponse.ok(null);
    }

    /**
     * 禁用用户账号。
     *
     * <p>禁用后用户不能登录，已有 token 在后续请求中也会因 tokenVersion 变化失效。</p>
     *
     * @param userId 目标用户 id
     * @return 空响应体
     */
    @PostMapping("/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long userId) {
        adminUserService.disableUser(userId);
        return ApiResponse.ok(null);
    }
}
