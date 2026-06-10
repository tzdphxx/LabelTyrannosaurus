package com.labelhub.modules.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DemoAccountProperties.class)
public class DemoAccountConfig {
}
