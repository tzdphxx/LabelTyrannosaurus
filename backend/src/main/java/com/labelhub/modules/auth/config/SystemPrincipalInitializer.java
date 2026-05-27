package com.labelhub.modules.auth.config;

import com.labelhub.modules.auth.service.SystemPrincipalService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Boot-time initializer for the BE-B owned system AI principal.
 *
 * <p>The initializer is idempotent so every application start repairs the
 * principal shape before BE-A can use it for AI audit records.</p>
 */
@Component
public class SystemPrincipalInitializer implements ApplicationRunner {

    private final SystemPrincipalService systemPrincipalService;

    public SystemPrincipalInitializer(SystemPrincipalService systemPrincipalService) {
        this.systemPrincipalService = systemPrincipalService;
    }

    @Override
    public void run(ApplicationArguments args) {
        systemPrincipalService.ensureSystemAgent();
    }
}
