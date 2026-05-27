package com.labelhub.modules.admin.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.AdminUserResponse;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
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

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserService(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 查询用户列表，默认由 Controller 传入 {@code includeSystem=false} 过滤系统用户。
     */
    public List<AdminUserResponse> listUsers(boolean includeSystem) {
        return userMapper.selectAdminUsers(includeSystem).stream()
                .map(user -> toResponse(user, userRoleMapper.selectRoleCodesByUserId(user.getId())))
                .toList();
    }

    /**
     * 替换用户角色并使旧 token 失效。
     *
     * <p>如果目标用户当前是最后一个 ADMIN，则禁止把 ADMIN 角色移除，避免系统失去
     * 后台管理入口。</p>
     */
    @Transactional
    public void changeRoles(Long userId, Set<RoleCode> roles) {
        UserEntity user = requireUser(userId);
        Set<RoleCode> oldRoles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        if (oldRoles.contains(RoleCode.ADMIN) && !roles.contains(RoleCode.ADMIN)
                && userRoleMapper.countUsersWithRole(RoleCode.ADMIN) <= 1) {
            throw new BusinessException(400101, "Cannot remove the last admin");
        }
        userRoleMapper.replaceRoles(userId, roles);
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

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400102, "User not found");
        }
        return user;
    }

    private AdminUserResponse toResponse(UserEntity user, Set<RoleCode> roles) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserType(),
                user.getEnabled(),
                user.getLoginEnabled(),
                user.getTokenVersion(),
                roles
        );
    }
}
