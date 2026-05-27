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
 * Maintains the system principal used by AI agent audit records.
 *
 * <p>BE-B owns the {@code system_ai_agent} user. BE-A reads this profile when
 * constructing system actor context, but must not create or mutate the user.</p>
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
     * Ensures the system AI principal exists and has the fixed non-login shape.
     *
     * @return profile that BE-A can use as the AI audit actor
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
            userMapper.updateById(user);
        }
        userRoleMapper.replaceRoles(user.getId(), SYSTEM_AGENT_ROLES);
        return toProfile(user);
    }

    /**
     * Returns the current system agent profile, repairing it first if needed.
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
