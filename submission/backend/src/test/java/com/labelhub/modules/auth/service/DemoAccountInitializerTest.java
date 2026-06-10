package com.labelhub.modules.auth.service;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.config.DemoAccountProperties;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoAccountInitializerTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void skipsDatabaseWritesWhenDemoAccountsAreDisabled() {
        DemoAccountInitializer initializer = new DemoAccountInitializer(
                disabledProperties(), userMapper, userRoleMapper, passwordEncoder);

        initializer.run(null);

        verify(userMapper, never()).selectByUsername(any());
        verify(userMapper, never()).insert(any(UserEntity.class));
        verify(userRoleMapper, never()).replaceRoles(any(), any());
    }

    @Test
    void createsAllDemoAccountsWithOneRoleEachWhenEnabled() {
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$demo");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(switch (user.getUsername()) {
                case "admin" -> 1L;
                case "owner" -> 2L;
                case "labeler" -> 3L;
                case "reviewer" -> 4L;
                default -> throw new IllegalArgumentException(user.getUsername());
            });
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        DemoAccountInitializer initializer = new DemoAccountInitializer(
                enabledProperties(), userMapper, userRoleMapper, passwordEncoder);

        initializer.run(null);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper, org.mockito.Mockito.times(4)).insert(userCaptor.capture());
        assertThat(userCaptor.getAllValues())
                .extracting(UserEntity::getUsername)
                .containsExactly("admin", "owner", "labeler", "reviewer");
        assertThat(userCaptor.getAllValues())
                .allSatisfy(user -> {
                    assertThat(user.getPasswordHash()).isEqualTo("$2a$demo");
                    assertThat(user.getUserType()).isEqualTo(UserType.USER);
                    assertThat(user.getLoginEnabled()).isTrue();
                    assertThat(user.getEnabled()).isTrue();
                    assertThat(user.getTokenVersion()).isEqualTo(1);
                });
        verify(userRoleMapper).replaceRoles(1L, Set.of(RoleCode.ADMIN));
        verify(userRoleMapper).replaceRoles(2L, Set.of(RoleCode.OWNER));
        verify(userRoleMapper).replaceRoles(3L, Set.of(RoleCode.LABELER));
        verify(userRoleMapper).replaceRoles(4L, Set.of(RoleCode.REVIEWER));
    }

    @Test
    void repairsExistingDemoAccountRoleWithoutChangingPassword() {
        UserEntity existing = new UserEntity();
        existing.setId(9L);
        existing.setUsername("admin");
        existing.setPasswordHash("$2a$existing");
        when(userMapper.selectByUsername("admin")).thenReturn(existing);
        DemoAccountInitializer initializer = new DemoAccountInitializer(
                singleAdminProperties(), userMapper, userRoleMapper, passwordEncoder);

        initializer.run(null);

        verify(userMapper, never()).insert(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
        verify(userRoleMapper).replaceRoles(9L, Set.of(RoleCode.ADMIN));
    }

    private static DemoAccountProperties disabledProperties() {
        return new DemoAccountProperties(false, "Password123", null);
    }

    private static DemoAccountProperties enabledProperties() {
        return new DemoAccountProperties(true, "Password123", null);
    }

    private static DemoAccountProperties singleAdminProperties() {
        return new DemoAccountProperties(true, "Password123",
                java.util.List.of(new DemoAccountProperties.Account(
                        "admin", "admin@labelhub.local", "Admin", RoleCode.ADMIN)));
    }
}
