package com.labelhub.modules.admin.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.AdminUserResponse;
import com.labelhub.modules.admin.dto.CreateReviewerRequest;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Admin 用户管理服务。
 *
 * <p>该服务只处理 BE-B 拥有的 users/user_roles 数据，不跨边界修改任务、
 * 标注或审核状态。</p>
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
    private static final List<RoleCode> ROLE_DISPLAY_PRIORITY = List.of(
            RoleCode.ADMIN, RoleCode.OWNER, RoleCode.REVIEWER, RoleCode.LABELER);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserMapper userMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询用户列表，默认由 Controller 传入 {@code includeSystem=false} 过滤系统用户。
     */
    public List<AdminUserResponse> listUsers(boolean includeSystem) {
        return userMapper.selectAdminUsers(includeSystem).stream()
                .map(user -> toResponse(user, userRoleMapper.selectRoleCodesByUserId(user.getId()), false))
                .toList();
    }

    /**
     * 替换用户角色并使旧 token 失效。
     *
     * <p>如果目标用户当前是最后一个 ADMIN，则禁止把 ADMIN 角色移除，避免系统失去
     * 后台管理入口。</p>
     */
    @Transactional
    public void changeRole(Long userId, RoleCode role) {
        if (role == null) {
            throw new BusinessException(400102, "用户角色不能为空");
        }
        UserEntity user = requireUser(userId);
        Set<RoleCode> oldRoles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        RoleCode oldRole = requireSingleRole(oldRoles);
        if (oldRole == RoleCode.ADMIN && role != RoleCode.ADMIN
                && userRoleMapper.countUsersWithRole(RoleCode.ADMIN) <= 1) {
            throw new BusinessException(400101, "不能移除最后一个管理员");
        }
        userRoleMapper.replaceRoles(userId, Set.of(role));
        userMapper.incrementTokenVersion(userId);
    }

    /**
     * 启用用户账号。
     */
    @Transactional
    public void enableUser(Long userId) {
        requireUser(userId);
        userMapper.setEnabled(userId, true);
    }

    /**
     * 禁用用户账号。
     */
    @Transactional
    public void disableUser(Long userId) {
        requireUser(userId);
        userMapper.setEnabled(userId, false);
    }

    @Transactional
    public AdminUserResponse createReviewer(CreateReviewerRequest request) {
        if (userMapper.selectByUsername(request.username()) != null
                || userMapper.selectByEmail(request.email()) != null) {
            throw new BusinessException(400102, "用户名或邮箱已存在");
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
        userRoleMapper.insert(new UserRoleEntity(user.getId(), RoleCode.REVIEWER));
        return toResponse(user, Set.of(RoleCode.REVIEWER), true);
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400102, "用户不存在");
        }
        return user;
    }

    private AdminUserResponse toResponse(UserEntity user, Set<RoleCode> roles, boolean strict) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserType(),
                user.getEnabled(),
                user.getLoginEnabled(),
                user.getTokenVersion(),
                strict ? requireSingleRole(roles) : displayRole(user, roles)
        );
    }

    private RoleCode displayRole(UserEntity user, Set<RoleCode> roles) {
        if (roles != null && roles.size() == 1) {
            return roles.iterator().next();
        }
        log.warn("User {} has invalid role data: {}", user.getId(), roles);
        if (roles != null) {
            for (RoleCode role : ROLE_DISPLAY_PRIORITY) {
                if (roles.contains(role)) {
                    return role;
                }
            }
        }
        return RoleCode.LABELER;
    }

    private RoleCode requireSingleRole(Set<RoleCode> roles) {
        if (roles == null || roles.size() != 1) {
            throw new BusinessException(400102, "用户必须且只能拥有一个角色");
        }
        return roles.iterator().next();
    }
}
