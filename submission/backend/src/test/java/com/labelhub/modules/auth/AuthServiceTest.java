package com.labelhub.modules.auth;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.AuthUserCacheService;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.JwtTokenService;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.dto.LoginRequest;
import com.labelhub.modules.auth.dto.RegisterRequest;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

class AuthServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final AuthUserCacheService authUserCacheService = mock(AuthUserCacheService.class);
    private final AuthService authService = new AuthService(
            userMapper, userRoleMapper, passwordEncoder, jwtTokenService, authUserCacheService);

    @Test
    void registerCreatesEnabledUserWithRequestedOwnerRole() {
        when(userMapper.selectByUsername("owner")).thenReturn(null);
        when(userMapper.selectByEmail("owner@example.com")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$hash");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(jwtTokenService.createAccessToken(10L, "owner", Set.of(RoleCode.OWNER), 1)).thenReturn("access");
        when(jwtTokenService.createRefreshToken(10L, "owner", 1)).thenReturn("refresh");

        var response = authService.register(new RegisterRequest("owner", "owner@example.com", "Password123", "OWNER"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.role()).isEqualTo(RoleCode.OWNER);
        verify(userMapper).insert(any(UserEntity.class));
        var roleCaptor = forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo(RoleCode.OWNER);
    }

    @Test
    void registerIssuesTokenWithRequestedLabelerRole() {
        when(userMapper.selectByUsername("labeler")).thenReturn(null);
        when(userMapper.selectByEmail("labeler@example.com")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$hash");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(30L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(jwtTokenService.createAccessToken(30L, "labeler", Set.of(RoleCode.LABELER), 1)).thenReturn("access");
        when(jwtTokenService.createRefreshToken(30L, "labeler", 1)).thenReturn("refresh");

        var response = authService.register(new RegisterRequest("labeler", "labeler@example.com", "Password123", "LABELER"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.role()).isEqualTo(RoleCode.LABELER);
        var roleCaptor = forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo(RoleCode.LABELER);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userMapper.selectByUsername("labeler")).thenReturn(new UserEntity());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("labeler", "new@example.com", "Password123", "LABELER")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void registerRejectsReviewerRole() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("reviewer", "reviewer@example.com", "Password123", "REVIEWER")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void registerRejectsAdminRole() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("admin", "admin@example.com", "Password123", "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void registerRejectsSystemAgentRole() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("agent", "agent@example.com", "Password123", "SYSTEM_AGENT")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void registerRejectsUnknownRole() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("bad", "bad@example.com", "Password123", "MANAGER")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void loginRejectsWrongPassword() {
        var user = user(10L, "labeler", true, true);
        when(userMapper.selectByUsernameOrEmail("labeler")).thenReturn(user);
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("labeler", "bad")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401001);
    }

    @Test
    void loginRejectsDisabledUser() {
        when(userMapper.selectByUsernameOrEmail("labeler")).thenReturn(user(10L, "labeler", false, true));

        assertThatThrownBy(() -> authService.login(new LoginRequest("labeler", "Password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401001);
    }

    @Test
    void loginRejectsSystemPrincipal() {
        var user = user(10L, "system_ai_agent", true, false);
        user.setUserType(UserType.SYSTEM);
        user.setPasswordHash(null);
        when(userMapper.selectByUsernameOrEmail("system_ai_agent")).thenReturn(user);

        assertThatThrownBy(() -> authService.login(new LoginRequest("system_ai_agent", "Password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401001);
    }

    @Test
    void loginReturnsSingleRole() {
        var user = user(30L, "labeler", true, true);
        when(userMapper.selectByUsernameOrEmail("labeler")).thenReturn(user);
        when(passwordEncoder.matches("Password123", "$2a$hash")).thenReturn(true);
        when(userRoleMapper.selectRoleCodesByUserId(30L)).thenReturn(Set.of(RoleCode.LABELER));
        when(jwtTokenService.createAccessToken(30L, "labeler", Set.of(RoleCode.LABELER), 1)).thenReturn("access");
        when(jwtTokenService.createRefreshToken(30L, "labeler", 1)).thenReturn("refresh");

        var response = authService.login(new LoginRequest("labeler", "Password123"));

        assertThat(response.role()).isEqualTo(RoleCode.LABELER);
    }

    @Test
    void loginRejectsUserWithoutExactlyOneRole() {
        var user = user(30L, "labeler", true, true);
        when(userMapper.selectByUsernameOrEmail("labeler")).thenReturn(user);
        when(passwordEncoder.matches("Password123", "$2a$hash")).thenReturn(true);
        when(userRoleMapper.selectRoleCodesByUserId(30L)).thenReturn(Set.of(RoleCode.LABELER, RoleCode.REVIEWER));

        assertThatThrownBy(() -> authService.login(new LoginRequest("labeler", "Password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void refreshRejectsStaleTokenVersion() {
        when(jwtTokenService.parseRefreshToken("refresh")).thenReturn(new JwtTokenService.TokenClaims(10L, "labeler", Set.of(), 1, true));
        var user = user(10L, "labeler", true, true);
        user.setTokenVersion(2);
        when(userMapper.selectById(10L)).thenReturn(user);

        assertThatThrownBy(() -> authService.refresh("refresh"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401001);
    }

    @Test
    void refreshReturnsSingleRole() {
        when(jwtTokenService.parseRefreshToken("refresh")).thenReturn(new JwtTokenService.TokenClaims(10L, "labeler", Set.of(), 1, true));
        var user = user(10L, "labeler", true, true);
        when(userMapper.selectById(10L)).thenReturn(user);
        when(userRoleMapper.selectRoleCodesByUserId(10L)).thenReturn(Set.of(RoleCode.LABELER));
        when(jwtTokenService.createAccessToken(10L, "labeler", Set.of(RoleCode.LABELER), 1)).thenReturn("access");
        when(jwtTokenService.createRefreshToken(10L, "labeler", 1)).thenReturn("refresh");

        var response = authService.refresh("refresh");

        assertThat(response.role()).isEqualTo(RoleCode.LABELER);
    }

    @Test
    void currentUserReturnsSingleRole() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        try {
            var response = authService.currentUser();

            assertThat(response.role()).isEqualTo(RoleCode.OWNER);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private static UserEntity user(Long id, String username, boolean enabled, boolean loginEnabled) {
        var user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("$2a$hash");
        user.setUserType(UserType.USER);
        user.setEnabled(enabled);
        user.setLoginEnabled(loginEnabled);
        user.setTokenVersion(1);
        return user;
    }
}
