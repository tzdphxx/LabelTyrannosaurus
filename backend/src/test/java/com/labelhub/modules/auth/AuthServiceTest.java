package com.labelhub.modules.auth;

import com.labelhub.common.exception.BusinessException;
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
    private final AuthService authService = new AuthService(userMapper, userRoleMapper, passwordEncoder, jwtTokenService);

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
        verify(userMapper).insert(any(UserEntity.class));
        var roleCaptor = forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo(RoleCode.OWNER);
    }

    @Test
    void registerIssuesTokenWithRequestedReviewerRole() {
        when(userMapper.selectByUsername("reviewer")).thenReturn(null);
        when(userMapper.selectByEmail("reviewer@example.com")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$hash");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(20L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(jwtTokenService.createAccessToken(20L, "reviewer", Set.of(RoleCode.REVIEWER), 1)).thenReturn("access");
        when(jwtTokenService.createRefreshToken(20L, "reviewer", 1)).thenReturn("refresh");

        var response = authService.register(new RegisterRequest("reviewer", "reviewer@example.com", "Password123", "REVIEWER"));

        assertThat(response.accessToken()).isEqualTo("access");
        var roleCaptor = forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo(RoleCode.REVIEWER);
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
