package com.labelhub.modules.auth.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.auth.dto.ChangePasswordRequest;
import com.labelhub.modules.auth.dto.LoginRequest;
import com.labelhub.modules.auth.dto.RefreshRequest;
import com.labelhub.modules.auth.dto.RegisterRequest;
import com.labelhub.modules.auth.dto.TokenResponse;
import com.labelhub.modules.auth.dto.UpdateProfileRequest;
import com.labelhub.modules.auth.dto.UserProfileResponse;
import com.labelhub.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口入口，负责注册、登录、刷新令牌和当前用户信息查询。
 *
 * <p>本 Controller 只做请求接收、参数校验和统一响应包装，认证业务规则由
 * {@link AuthService} 维护。</p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "认证", description = "注册、登录、刷新令牌和当前用户信息")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口。
     *
     * <p>公开访问。注册成功后创建普通用户，默认授予 {@code LABELER} 角色，
     * 密码仅保存 BCrypt hash，并直接返回一组登录令牌。</p>
     *
     * @param request 注册请求，包含 username、email、password
     * @return accessToken、refreshToken 和当前 tokenVersion
     */
    @PostMapping("/auth/register")
    @SecurityRequirements
    @Operation(summary = "用户注册", description = "创建普通用户，按 role 参数授予 LABELER 或 OWNER，并返回 accessToken 和 refreshToken。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 用户登录接口。
     *
     * <p>公开访问。仅允许 {@code enabled=true}、{@code loginEnabled=true}
     * 的普通用户登录，系统用户不能通过该接口登录。</p>
     *
     * @param request 登录请求，account 支持用户名或邮箱
     * @return accessToken、refreshToken 和当前 tokenVersion
     */
    @PostMapping("/auth/login")
    @SecurityRequirements
    @Operation(summary = "用户登录", description = "支持用户名或邮箱登录。仅普通且启用登录的用户可以获取令牌。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 刷新令牌接口。
     *
     * <p>公开访问但必须提供有效 refreshToken。服务端会校验 refreshToken
     * 中的 tokenVersion 是否与数据库当前值一致，角色变更或禁用后旧令牌会失效。</p>
     *
     * @param request refreshToken 请求体
     * @return 新的 accessToken、refreshToken 和当前 tokenVersion
     */
    @PostMapping("/auth/refresh")
    @SecurityRequirements
    @Operation(summary = "刷新令牌", description = "使用有效 refreshToken 换取新的 accessToken 和 refreshToken。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * 当前用户信息接口。
     *
     * <p>需要已认证用户。返回前端恢复登录态和渲染权限菜单所需的最小用户信息，
     * 不返回密码 hash、令牌或其他敏感字段。</p>
     *
     * @return 当前用户 id、用户名、邮箱和角色集合
     */
    @GetMapping("/users/me")
    @Operation(summary = "当前用户信息", description = "返回当前认证用户的最小资料和角色集合。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<UserProfileResponse> currentUser() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PutMapping("/users/me/password")
    @Operation(summary = "修改密码", description = "校验旧密码后更新为新密码，成功后旧令牌失效需重新登录。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUserContext.getUserId(), request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/users/me/profile")
    @Operation(summary = "更新个人信息", description = "更新当前用户的显示名称和邮箱。邮箱需全局唯一。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(CurrentUserContext.getUserId(), request);
        return ApiResponse.ok(null);
    }
}
