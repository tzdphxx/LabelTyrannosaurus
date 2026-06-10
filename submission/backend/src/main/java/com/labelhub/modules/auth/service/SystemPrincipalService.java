package com.labelhub.modules.auth.service;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.dto.SystemAgentProfile;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 维护 AI Agent 审计使用的系统主体。
 *
 * <p>{@code system_ai_agent} 用户由 BE-B 创建和维护。BE-A 构造系统审计主体时只读取该资料，
 * 不能创建或修改该用户。</p>
 */
@Service
public class SystemPrincipalService {

    public static final String SYSTEM_AGENT_USERNAME = "system_ai_agent";
    private static final String SYSTEM_AGENT_EMAIL = "system_ai_agent@labelhub.local";
    private static final String SYSTEM_AGENT_DISPLAY_NAME = "System AI Agent";
    private static final Set<RoleCode> SYSTEM_AGENT_ROLES = Set.of(RoleCode.SYSTEM_AGENT);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public SystemPrincipalService(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 确保系统 AI 主体存在，并保持固定的不可登录用户形态。
     *
     * @return BE-A 可用于 AI 审计主体的资料
     */
    @Transactional
    public SystemAgentProfile ensureSystemAgent() {
        UserEntity user = userMapper.selectByUsername(SYSTEM_AGENT_USERNAME);
        if (user == null) {
            user = new UserEntity();
            user.setUsername(SYSTEM_AGENT_USERNAME);
            user.setTokenVersion(1);
            applyFixedFields(user);
            userMapper.insert(user);
        } else {
            applyFixedFields(user);
            userMapper.repairSystemPrincipal(user.getId(), SYSTEM_AGENT_EMAIL, SYSTEM_AGENT_DISPLAY_NAME);
        }
        userRoleMapper.replaceRoles(user.getId(), SYSTEM_AGENT_ROLES);
        return toProfile(user);
    }

    /**
     * 返回当前系统 Agent 资料；如果字段被改动，会先修正为固定形态。
     */
    @Transactional
    public SystemAgentProfile getSystemAgentProfile() {
        return ensureSystemAgent();
    }

    private void applyFixedFields(UserEntity user) {
        user.setEmail(SYSTEM_AGENT_EMAIL);
        user.setPasswordHash(null);
        user.setUserType(UserType.SYSTEM);
        user.setLoginEnabled(false);
        user.setEnabled(true);
        user.setDisplayName(SYSTEM_AGENT_DISPLAY_NAME);
    }

    private SystemAgentProfile toProfile(UserEntity user) {
        return new SystemAgentProfile(user.getId(), user.getUsername(), SYSTEM_AGENT_ROLES);
    }
}
