package com.labelhub.modules.auth.config;

import com.labelhub.modules.auth.service.SystemPrincipalService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时初始化 BE-B 维护的系统 AI 主体。
 *
 * <p>初始化过程具备幂等性，确保 BE-A 使用该主体写 AI 审计前，用户字段已恢复到固定形态。</p>
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
