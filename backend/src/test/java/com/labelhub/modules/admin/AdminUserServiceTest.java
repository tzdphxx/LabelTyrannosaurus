package com.labelhub.modules.admin;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.service.AdminUserService;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final AdminUserService adminUserService = new AdminUserService(userMapper, userRoleMapper);

    @Test
    void changeRolesIncrementsTokenVersion() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 1));
        when(userRoleMapper.countUsersWithRole(RoleCode.ADMIN)).thenReturn(2L);

        adminUserService.changeRoles(10L, Set.of(RoleCode.OWNER, RoleCode.REVIEWER));

        verify(userRoleMapper).replaceRoles(10L, Set.of(RoleCode.OWNER, RoleCode.REVIEWER));
        verify(userMapper).incrementTokenVersion(10L);
    }

    @Test
    void changeRolesRejectsRemovingLastAdmin() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 1));
        when(userRoleMapper.selectRoleCodesByUserId(10L)).thenReturn(Set.of(RoleCode.ADMIN));
        when(userRoleMapper.countUsersWithRole(RoleCode.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.changeRoles(10L, Set.of(RoleCode.LABELER)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400101);
    }

    @Test
    void disableUserIncrementsTokenVersion() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 1));

        adminUserService.disableUser(10L);

        verify(userMapper).setEnabled(10L, false);
    }

    @Test
    void listUsersExcludesSystemUsersByDefault() {
        adminUserService.listUsers(false);

        verify(userMapper).selectAdminUsers(false);
    }

    private static UserEntity user(Long id, int tokenVersion) {
        var user = new UserEntity();
        user.setId(id);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
