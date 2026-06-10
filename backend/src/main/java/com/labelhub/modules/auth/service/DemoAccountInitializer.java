package com.labelhub.modules.auth.service;

import com.labelhub.modules.auth.config.DemoAccountProperties;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserType;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private final DemoAccountProperties properties;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountInitializer(DemoAccountProperties properties,
                                  UserMapper userMapper,
                                  UserRoleMapper userRoleMapper,
                                  PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        for (DemoAccountProperties.Account account : properties.resolvedAccounts()) {
            ensureAccount(account);
        }
    }

    private void ensureAccount(DemoAccountProperties.Account account) {
        UserEntity user = userMapper.selectByUsername(account.username());
        if (user == null) {
            user = new UserEntity();
            user.setUsername(account.username());
            user.setEmail(account.email());
            user.setPasswordHash(passwordEncoder.encode(properties.resolvedPassword()));
            user.setTokenVersion(1);
            applyLoginFields(user, account);
            userMapper.insert(user);
        } else {
            applyLoginFields(user, account);
            if (user.getTokenVersion() == null) {
                user.setTokenVersion(1);
            }
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(properties.resolvedPassword()));
            }
            userMapper.updateById(user);
        }
        userRoleMapper.replaceRoles(user.getId(), Set.of(account.role()));
    }

    private void applyLoginFields(UserEntity user, DemoAccountProperties.Account account) {
        user.setUserType(UserType.USER);
        user.setLoginEnabled(true);
        user.setEnabled(true);
        user.setDisplayName(account.displayName());
    }
}
