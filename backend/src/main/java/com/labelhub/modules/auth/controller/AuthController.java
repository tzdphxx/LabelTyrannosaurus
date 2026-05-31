package com.labelhub.modules.auth.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.auth.dto.LoginRequest;
import com.labelhub.modules.auth.dto.RefreshRequest;
import com.labelhub.modules.auth.dto.RegisterRequest;
import com.labelhub.modules.auth.dto.TokenResponse;
import com.labelhub.modules.auth.dto.UserProfileResponse;
import com.labelhub.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ApiResponse<UserProfileResponse> currentUser() {
        return ApiResponse.ok(authService.currentUser());
    }
}
