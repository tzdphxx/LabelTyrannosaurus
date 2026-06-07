package com.labelhub.modules.admin;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.CreateReviewerRequest;
import com.labelhub.modules.admin.service.AdminUserService;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

class AdminUserServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminUserService adminUserService = new AdminUserService(userMapper, userRoleMapper, passwordEncoder);

    @Test
    void listUsersReturnsSingleRole() {
        when(userMapper.selectAdminUsers(false)).thenReturn(List.of(user(10L, 1)));
        when(userRoleMapper.selectRoleCodesByUserId(10L)).thenReturn(Set.of(RoleCode.ADMIN));

        var responses = adminUserService.listUsers(false);

        assertThat(responses).singleElement()
                .extracting("role")
                .isEqualTo(RoleCode.ADMIN);
    }

    @Test
    void disableUserIncrementsTokenVersion() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 1));

        adminUserService.disableUser(10L);

        verify(userMapper).setEnabled(10L, false);
    }

    @Test
    void changeRoleReplacesSingleRoleAndInvalidatesExistingTokens() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 1));
        when(userRoleMapper.selectRoleCodesByUserId(10L)).thenReturn(Set.of(RoleCode.LABELER));

        adminUserService.changeRole(10L, RoleCode.REVIEWER);

        verify(userRoleMapper).replaceRoles(10L, Set.of(RoleCode.REVIEWER));
        verify(userMapper).incrementTokenVersion(10L);
    }

    @Test
    void listUsersExcludesSystemUsersByDefault() {
        adminUserService.listUsers(false);

        verify(userMapper).selectAdminUsers(false);
    }

    @Test
    void listUsersToleratesInvalidRoleDataWithoutFailingWholePage() {
        when(userMapper.selectAdminUsers(false)).thenReturn(List.of(user(10L, 1), user(11L, 1), user(12L, 1)));
        when(userRoleMapper.selectRoleCodesByUserId(10L)).thenReturn(Set.of(RoleCode.LABELER));
        when(userRoleMapper.selectRoleCodesByUserId(11L)).thenReturn(Set.of(RoleCode.REVIEWER, RoleCode.OWNER));
        when(userRoleMapper.selectRoleCodesByUserId(12L)).thenReturn(Set.of());

        var responses = adminUserService.listUsers(false);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).role()).isEqualTo(RoleCode.LABELER);
        assertThat(responses.get(1).role()).isEqualTo(RoleCode.OWNER);
        assertThat(responses.get(2).role()).isEqualTo(RoleCode.LABELER);
    }

    @Test
    void createReviewerCreatesEnabledReviewerAccount() {
        when(userMapper.selectByUsername("reviewer")).thenReturn(null);
        when(userMapper.selectByEmail("reviewer@example.com")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$hash");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(20L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        var response = adminUserService.createReviewer(
                new CreateReviewerRequest("reviewer", "reviewer@example.com", "Password123"));

        assertThat(response.role()).isEqualTo(RoleCode.REVIEWER);
        var roleCaptor = forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo(RoleCode.REVIEWER);
    }

    @Test
    void createReviewerRejectsDuplicateAccount() {
        when(userMapper.selectByUsername("reviewer")).thenReturn(new UserEntity());

        assertThatThrownBy(() -> adminUserService.createReviewer(
                new CreateReviewerRequest("reviewer", "reviewer@example.com", "Password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    private static UserEntity user(Long id, int tokenVersion) {
        var user = new UserEntity();
        user.setId(id);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
