package com.labelhub.modules.auth.config;

import com.labelhub.common.security.RoleCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "labelhub.demo-accounts")
public record DemoAccountProperties(
        boolean enabled,
        String password,
        List<Account> accounts
) {

    private static final String DEFAULT_PASSWORD = "Password123";
    private static final List<Account> DEFAULT_ACCOUNTS = List.of(
            new Account("admin", "admin@labelhub.local", "Admin", RoleCode.ADMIN),
            new Account("owner", "owner@labelhub.local", "Owner", RoleCode.OWNER),
            new Account("labeler", "labeler@labelhub.local", "Labeler", RoleCode.LABELER),
            new Account("reviewer", "reviewer@labelhub.local", "Reviewer", RoleCode.REVIEWER)
    );

    public String resolvedPassword() {
        return password == null || password.isBlank() ? DEFAULT_PASSWORD : password;
    }

    public List<Account> resolvedAccounts() {
        return accounts == null || accounts.isEmpty() ? DEFAULT_ACCOUNTS : List.copyOf(accounts);
    }

    public record Account(
            String username,
            String email,
            String displayName,
            RoleCode role
    ) {
    }
}
