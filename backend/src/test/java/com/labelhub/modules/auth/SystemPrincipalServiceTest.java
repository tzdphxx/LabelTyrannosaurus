package com.labelhub.modules.auth;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.modules.auth.service.SystemPrincipalService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemPrincipalServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final SystemPrincipalService systemPrincipalService = new SystemPrincipalService(userMapper, userRoleMapper);

    @Test
    void ensureSystemAgentCreatesFixedSystemUserAndRoleWhenMissing() {
        when(userMapper.selectByUsername(SystemPrincipalService.SYSTEM_AGENT_USERNAME)).thenReturn(null);
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        var profile = systemPrincipalService.ensureSystemAgent();

        assertThat(profile.userId()).isEqualTo(99L);
        assertThat(profile.username()).isEqualTo(SystemPrincipalService.SYSTEM_AGENT_USERNAME);
        assertThat(profile.roles()).containsExactly(RoleCode.SYSTEM_AGENT);
        verify(userRoleMapper).replaceRoles(99L, Set.of(RoleCode.SYSTEM_AGENT));
    }

    @Test
    void ensureSystemAgentRepairsExistingSystemUserFields() {
        UserEntity existing = new UserEntity();
        existing.setId(100L);
        existing.setUsername(SystemPrincipalService.SYSTEM_AGENT_USERNAME);
        existing.setEmail("wrong@example.com");
        existing.setPasswordHash("$2a$hash");
        existing.setUserType(UserType.USER);
        existing.setLoginEnabled(true);
        existing.setEnabled(false);
        existing.setTokenVersion(3);
        when(userMapper.selectByUsername(SystemPrincipalService.SYSTEM_AGENT_USERNAME)).thenReturn(existing);

        var profile = systemPrincipalService.ensureSystemAgent();

        assertThat(profile.userId()).isEqualTo(100L);
        assertThat(existing.getUserType()).isEqualTo(UserType.SYSTEM);
        assertThat(existing.getLoginEnabled()).isFalse();
        assertThat(existing.getEnabled()).isTrue();
        assertThat(existing.getPasswordHash()).isNull();
        verify(userMapper).updateById(existing);
        verify(userRoleMapper).replaceRoles(100L, Set.of(RoleCode.SYSTEM_AGENT));
    }

    @Test
    void getSystemAgentProfileReturnsExistingProfileWithoutCreating() {
        UserEntity existing = new UserEntity();
        existing.setId(101L);
        existing.setUsername(SystemPrincipalService.SYSTEM_AGENT_USERNAME);
        when(userMapper.selectByUsername(SystemPrincipalService.SYSTEM_AGENT_USERNAME)).thenReturn(existing);

        var profile = systemPrincipalService.getSystemAgentProfile();

        assertThat(profile.userId()).isEqualTo(101L);
        assertThat(profile.roles()).containsExactly(RoleCode.SYSTEM_AGENT);
    }
}
