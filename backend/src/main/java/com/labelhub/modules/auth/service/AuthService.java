package com.labelhub.modules.auth.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.JwtTokenService;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.dto.LoginRequest;
import com.labelhub.modules.auth.dto.RegisterRequest;
import com.labelhub.modules.auth.dto.TokenResponse;
import com.labelhub.modules.auth.dto.UserProfileResponse;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Auth/RBAC 业务服务。
 *
 * <p>负责维护注册、登录、刷新和当前用户查询的核心规则。密码、令牌和用户状态校验
 * 都集中在这里，Controller 不直接处理安全细节。</p>
 */
@Service
public class AuthService {

    private static final Set<RoleCode> DEFAULT_REGISTER_ROLES = Set.of(RoleCode.LABELER);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper,
                       UserRoleMapper userRoleMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 注册普通用户并签发令牌。
     *
     * <p>注册用户固定为 {@code USER} 类型，默认可登录、可用，并授予 {@code LABELER}
     * 角色。用户名和邮箱任一重复都会返回参数非法错误。</p>
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userMapper.selectByUsername(request.username()) != null || userMapper.selectByEmail(request.email()) != null) {
            throw new BusinessException(400102, "Username or email already exists");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(UserType.USER);
        user.setEnabled(true);
        user.setLoginEnabled(true);
        user.setTokenVersion(1);
        userMapper.insert(user);
        userRoleMapper.insert(new UserRoleEntity(user.getId(), RoleCode.LABELER));
        return issueTokens(user, DEFAULT_REGISTER_ROLES);
    }

    /**
     * 校验账号密码并签发令牌。
     *
     * <p>只允许普通用户登录，禁用账号、禁止登录账号和系统用户统一返回未认证错误，
     * 避免向外暴露账号状态细节。</p>
     */
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userMapper.selectByUsernameOrEmail(request.account());
        if (!canLogin(user) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401001, "Invalid account or password");
        }
        Set<RoleCode> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        userMapper.updateLastLoginAt(user.getId());
        return issueTokens(user, roles);
    }

    /**
     * 使用 refreshToken 换取新令牌。
     *
     * <p>refreshToken 内的 tokenVersion 必须与数据库一致。Admin 修改角色或启停账号后，
     * 数据库 tokenVersion 递增，旧 refreshToken 会被拒绝。</p>
     */
    public TokenResponse refresh(String refreshToken) {
        JwtTokenService.TokenClaims claims = jwtTokenService.parseRefreshToken(refreshToken);
        UserEntity user = userMapper.selectById(claims.userId());
        if (!canLogin(user) || !user.getTokenVersion().equals(claims.tokenVersion())) {
            throw new BusinessException(401001, "Invalid refresh token");
        }
        Set<RoleCode> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        return issueTokens(user, roles);
    }

    /**
     * 获取当前请求认证用户的最小资料。
     */
    public UserProfileResponse currentUser() {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        return new UserProfileResponse(currentUser.userId(), currentUser.username(), currentUser.email(), currentUser.roles());
    }

    private TokenResponse issueTokens(UserEntity user, Set<RoleCode> roles) {
        return new TokenResponse(
                jwtTokenService.createAccessToken(user.getId(), user.getUsername(), roles, user.getTokenVersion()),
                jwtTokenService.createRefreshToken(user.getId(), user.getUsername(), user.getTokenVersion()),
                user.getTokenVersion()
        );
    }

    private boolean canLogin(UserEntity user) {
        return user != null
                && user.getUserType() == UserType.USER
                && Boolean.TRUE.equals(user.getEnabled())
                && Boolean.TRUE.equals(user.getLoginEnabled())
                && user.getPasswordHash() != null;
    }
}
