package com.labelhub.modules.admin.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.admin.dto.AdminUserResponse;
import com.labelhub.modules.admin.dto.CreateReviewerRequest;
import com.labelhub.modules.admin.dto.UpdateUserRolesRequest;
import com.labelhub.modules.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * <p>类级别要求 {@code ADMIN} 角色。接口用于用户列表、审核员创建和账号启停，
 * 任何会影响用户权限或登录态的操作都必须让旧 token 失效。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "用户管理", description = "管理员用户列表、审核员创建和账号启停")
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
    @Operation(summary = "用户列表", description = "查询后台用户列表，默认排除系统用户。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<AdminUserResponse>> listUsers(@RequestParam(defaultValue = "false") boolean includeSystem) {
        return ApiResponse.ok(adminUserService.listUsers(includeSystem));
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
    @Operation(summary = "启用用户", description = "启用账号并递增 tokenVersion。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
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
    @Operation(summary = "禁用用户", description = "禁用账号并递增 tokenVersion，使已有令牌失效。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<Void> disableUser(@PathVariable Long userId) {
        adminUserService.disableUser(userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reviewers")
    public ApiResponse<AdminUserResponse> createReviewer(@Valid @RequestBody CreateReviewerRequest request) {
        return ApiResponse.ok(adminUserService.createReviewer(request));
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "更新用户角色", description = "替换用户的单一角色并递增 tokenVersion，使已有令牌失效。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<Void> updateRole(@PathVariable Long userId,
                                        @Valid @RequestBody UpdateUserRolesRequest request) {
        adminUserService.changeRole(userId, request.role());
        return ApiResponse.ok(null);
    }
}
